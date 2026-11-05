/**
 * Service Worker —— 让网站可以"添加到主屏幕"并离线打开外壳
 *
 * 缓存策略刻意保守，避免把自己坑了：
 *   · /api、/library、/uploads 一律不走缓存
 *     —— 接口要实时数据、音频和歌词文件体积大且容易被替换
 *   · 页面导航：网络优先，断网时回退到缓存的 index.html
 *   · 静态资源（js/css/图标）：缓存优先 + 后台更新
 */
const CACHE = 'neon-music-v1'

const SHELL = [
  '/',
  '/index.html',
  '/manifest.webmanifest',
  '/icon-192.png',
  '/icon-512.png'
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(SHELL).catch(() => {}))
      .then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (event) => {
  const req = event.request
  if (req.method !== 'GET') return

  let url
  try {
    url = new URL(req.url)
  } catch (e) {
    return
  }

  // 只处理本站请求
  if (url.origin !== self.location.origin) return
  // 接口 / 音乐库 / 上传文件：不缓存
  if (/^\/(api|library|uploads)\//.test(url.pathname)) return

  // 页面导航：网络优先，离线回退
  if (req.mode === 'navigate') {
    event.respondWith(fetch(req).catch(() => caches.match('/index.html')))
    return
  }

  // 静态资源：缓存优先，同时后台更新
  event.respondWith(
    caches.match(req).then((hit) => {
      const network = fetch(req)
        .then((res) => {
          if (res && res.status === 200 && res.type === 'basic') {
            const copy = res.clone()
            caches.open(CACHE).then((cache) => cache.put(req, copy)).catch(() => {})
          }
          return res
        })
        .catch(() => hit)
      return hit || network
    })
  )
})
