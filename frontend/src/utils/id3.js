/**
 * 极简 ID3v2 标签解析器（只读，不依赖任何第三方库）
 *
 * 为什么需要：
 *   网易云下载的 mp3 文件名是 `002OKIox28ad9a.mp3` 这种 ID，
 *   光看文件名根本不知道是什么歌。真正的歌名/歌手/专辑/封面
 *   都藏在文件头部的 ID3v2 标签里。
 *
 * 支持：ID3v2.3 / ID3v2.4（覆盖绝大多数 mp3）
 * 不支持的：ID3v2.2（90 年代的格式，帧 ID 只有 3 字节）、ID3v1（文件末尾，只存纯文本）
 *
 * 用到的帧：
 *   TIT2 标题   TPE1 歌手   TALB 专辑   TCON 流派   APIC 封面
 */

const TEXT_ENCODINGS = {
  0: 'iso-8859-1',  // Latin-1
  1: 'utf-16',      // 带 BOM
  2: 'utf-16be',    // 无 BOM
  3: 'utf-8'
}

/** ID3 的长度字段是「syncsafe」：每字节只用低 7 位 */
function syncsafeToInt(bytes, offset) {
  return (bytes[offset] & 0x7f) * 0x200000 +
    (bytes[offset + 1] & 0x7f) * 0x4000 +
    (bytes[offset + 2] & 0x7f) * 0x80 +
    (bytes[offset + 3] & 0x7f)
}

/** 大端 32 位整数（v2.3 帧长度用这个） */
function uint32BE(bytes, offset) {
  return ((bytes[offset] & 0xff) << 24) |
    ((bytes[offset + 1] & 0xff) << 16) |
    ((bytes[offset + 2] & 0xff) << 8) |
    (bytes[offset + 3] & 0xff)
}

function decodeText(bytes, start, end, encoding) {
  if (end <= start) return ''
  const enc = TEXT_ENCODINGS[encoding] || 'iso-8859-1'
  try {
    let text = new TextDecoder(enc, { fatal: false }).decode(bytes.subarray(start, end))
    // 去掉字符串终止符和多余空白
    return text.replace(/\u0000+$/g, '').replace(/^\u0000+/, '').trim()
  } catch (e) {
    return ''
  }
}

/**
 * 解析 ID3v2 标签
 * @param {ArrayBuffer} buffer 文件开头的字节（至少包含整个标签）
 * @returns {{title?:string, artist?:string, album?:string, genre?:string, cover?:{mime:string, data:Uint8Array}}|null}
 */
export function parseId3(buffer) {
  const bytes = new Uint8Array(buffer)
  if (bytes.length < 10) return null
  // 头部必须是 "ID3"
  if (bytes[0] !== 0x49 || bytes[1] !== 0x44 || bytes[2] !== 0x33) return null

  const major = bytes[3]
  if (major !== 3 && major !== 4) return null   // 只处理 v2.3 / v2.4

  const tagSize = syncsafeToInt(bytes, 6)
  const end = Math.min(10 + tagSize, bytes.length)
  const result = {}

  let pos = 10
  while (pos + 10 <= end) {
    const id = String.fromCharCode(bytes[pos], bytes[pos + 1], bytes[pos + 2], bytes[pos + 3])
    // 帧 ID 必须是 A-Z0-9，遇到非法就说明已经到填充区了
    if (!/^[A-Z0-9]{4}$/.test(id)) break

    // v2.4 帧长度是 syncsafe，v2.3 是普通大端
    const size = major === 4 ? syncsafeToInt(bytes, pos + 4) : uint32BE(bytes, pos + 4)
    if (size <= 0) break
    const dataStart = pos + 10
    const dataEnd = Math.min(dataStart + size, end)
    if (dataEnd <= dataStart) break

    if (id === 'TIT2' || id === 'TPE1' || id === 'TALB' || id === 'TCON') {
      const encoding = bytes[dataStart]
      const text = decodeText(bytes, dataStart + 1, dataEnd, encoding)
      if (text) {
        if (id === 'TIT2') result.title = text
        else if (id === 'TPE1') result.artist = text
        else if (id === 'TALB') result.album = text
        else if (id === 'TCON') result.genre = text.replace(/^\(\d+\)/, '').trim()
      }
    } else if (id === 'APIC') {
      const encoding = bytes[dataStart]
      let p = dataStart + 1
      // MIME：Latin-1，以 \0 结尾
      let mime = ''
      while (p < dataEnd && bytes[p] !== 0) { mime += String.fromCharCode(bytes[p]); p++ }
      p++                        // 跳过 MIME 的 \0
      p++                        // 跳过图片类型（1 字节）
      // 描述文字：按 encoding 走，第一个 \0 结束（UTF-16 是两个 \0）
      const descEnd = (() => {
        let q = p
        while (q < dataEnd) {
          if (encoding === 1 || encoding === 2) {
            if (bytes[q] === 0 && bytes[q + 1] === 0) return q
            q += 2
          } else {
            if (bytes[q] === 0) return q
            q++
          }
        }
        return q
      })()
      const imgStart = descEnd + (encoding === 1 || encoding === 2 ? 2 : 1)
      if (imgStart < dataEnd) {
        result.cover = { mime: mime || 'image/jpeg', data: bytes.slice(imgStart, dataEnd) }
      }
    }
    pos = dataStart + size
  }

  return (result.title || result.artist || result.album || result.cover) ? result : null
}

/**
 * 从 File 读取 ID3 标签
 * 只读文件开头一段（标签 + 内嵌封面通常都在前 1MB 内），避免把 180 个文件整个读进内存
 */
export async function readId3FromFile(file) {
  try {
    const slice = file.slice(0, Math.min(file.size, 1024 * 1024))
    const buf = await slice.arrayBuffer()
    return parseId3(buf)
  } catch (e) {
    return null
  }
}
