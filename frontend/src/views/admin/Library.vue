<template>
  <div class="library-scan">
    <div class="page-header">
      <div>
        <h2>音乐库扫描</h2>
        <p class="sub">直接读取硬盘上的音乐目录，不需要通过浏览器逐个上传（文件不会被复制）</p>
      </div>
    </div>

    <!-- 配置状态 -->
    <div class="config-card">
      <div class="config-row">
        <span class="label">音乐目录</span>
        <span class="value mono">{{ config.audioDir || '（未配置）' }}</span>
        <span class="badge" :class="config.audioDirExists ? 'ok' : 'bad'">
          {{ config.audioDirExists ? '✓ 可访问' : '✗ 不存在' }}
        </span>
      </div>
      <div class="config-row">
        <span class="label">歌词目录</span>
        <span class="value mono">{{ config.lyricDir || '（未配置，将不导入歌词）' }}</span>
        <span v-if="config.lyricDir" class="badge" :class="config.lyricDirExists ? 'ok' : 'bad'">
          {{ config.lyricDirExists ? '✓ 可访问' : '✗ 不存在' }}
        </span>
      </div>

      <div v-if="!config.enabled" class="hint warn">
        尚未启用。请在项目根目录的 <code>.env</code> 里配置下面两项，然后重启后端：
        <pre>LIBRARY_AUDIO_DIR=E:/音乐
LIBRARY_LYRIC_DIR=E:/音乐歌词</pre>
      </div>

      <div class="actions" v-if="config.enabled">
        <button class="btn btn-ghost" :disabled="running" @click="run(true)">
          {{ running && dryRun ? '预检中…' : '预检（只看会新增多少）' }}
        </button>
        <button class="btn btn-primary" :disabled="running" @click="run(false)">
          {{ running && !dryRun ? '扫描中…' : '开始扫描并导入' }}
        </button>
      </div>
    </div>

    <!-- 扫描中 -->
    <div v-if="running" class="running">
      <div class="spinner"></div>
      <p>正在遍历目录并解析标签，歌多时需要几十秒，请不要关闭页面…</p>
    </div>

    <!-- 结果 -->
    <div v-if="result" class="result">
      <div class="stats">
        <div class="stat"><b class="ok">{{ result.added }}</b><span>新增</span></div>
        <div class="stat"><b>{{ result.updated }}</b><span>更新</span></div>
        <div class="stat"><b>{{ result.skipped }}</b><span>已存在跳过</span></div>
        <div class="stat"><b class="bad">{{ result.failed }}</b><span>失败</span></div>
        <div class="stat"><b class="warn">{{ result.missing }}</b><span>文件已失效</span></div>
      </div>

      <p class="meta">
        目录 <code>{{ result.audioDir }}</code> ·
        发现音频 {{ result.totalAudioFiles }} 个 · 歌词 {{ result.totalLyricFiles }} 个
        <span v-if="result.dryRun" class="dry-tag">（本次是预检，未写入数据库）</span>
      </p>

      <div v-if="!result.dryRun && result.added > 0" class="hint ok-hint">
        已导入 {{ result.added }} 首，去
        <router-link to="/admin/songs">歌曲管理</router-link>
        或 <router-link to="/">首页</router-link> 查看。
      </div>

      <!-- 明细 -->
      <div v-if="result.samples && result.samples.length" class="data-card">
        <div class="card-title">处理明细（最多显示 200 条）</div>
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr><th style="width: 80px">结果</th><th>文件</th><th style="width: 180px">歌手</th></tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in result.samples" :key="i" :class="rowClass(s.action)">
                <td>{{ s.action }}</td>
                <td class="mono ellipsis" :title="s.file">{{ s.file }}</td>
                <td class="ellipsis">{{ s.extra || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 失效条目 -->
      <div v-if="result.missingSamples && result.missingSamples.length" class="data-card">
        <div class="card-title">
          记录还在但文件已消失（{{ result.missing }} 条，仅提示，不会自动删除）
        </div>
        <div class="table-scroll" style="max-height: 240px">
          <table class="data-table">
            <thead><tr><th style="width: 70px">ID</th><th>歌名</th><th>地址</th></tr></thead>
            <tbody>
              <tr v-for="m in result.missingSamples" :key="m.id">
                <td>{{ m.id }}</td>
                <td>{{ m.name }}</td>
                <td class="mono ellipsis" :title="m.url">{{ m.url }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLibraryConfig, scanLibrary } from '@/api/library'

const config = ref({ audioDir: '', lyricDir: '', enabled: false, audioDirExists: false, lyricDirExists: false })
const running = ref(false)
const dryRun = ref(false)
const result = ref(null)

const rowClass = (action) => ({
  'row-add': action === '新增',
  'row-update': action === '更新',
  'row-fail': action === '失败'
})

const loadConfig = async () => {
  try {
    const res = await getLibraryConfig()
    if (res.data) config.value = res.data
  } catch (e) {
    ElMessage.error('读取音乐库配置失败')
  }
}

const run = async (isDry) => {
  running.value = true
  dryRun.value = isDry
  result.value = null
  try {
    const res = await scanLibrary(isDry)
    result.value = res.data
    if (isDry) {
      ElMessage.success(`预检完成：将新增 ${res.data.added} 首`)
    } else {
      ElMessage.success(`扫描完成：新增 ${res.data.added} 首`)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '扫描失败')
  } finally {
    running.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.library-scan { padding: 0 4px; }
.page-header { margin-bottom: 18px; }
.page-header h2 { font-size: 20px; font-weight: 600; color: #fff; }
.page-header .sub { font-size: 12px; color: rgba(255,255,255,0.4); margin-top: 4px; }

.config-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; padding: 18px 20px; margin-bottom: 16px; }
.config-row { display: flex; align-items: center; gap: 12px; padding: 7px 0; font-size: 13px; }
.config-row .label { width: 80px; color: rgba(255,255,255,0.45); flex-shrink: 0; }
.config-row .value { color: rgba(255,255,255,0.8); }
.badge { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.badge.ok { background: rgba(67,233,123,0.15); color: #43e97b; }
.badge.bad { background: rgba(245,87,108,0.15); color: #f5576c; }

.mono { font-family: Consolas, Monaco, monospace; }
.ellipsis { max-width: 520px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.hint { margin-top: 12px; padding: 10px 14px; border-radius: 8px; font-size: 12px; line-height: 1.8; }
.hint.warn { background: rgba(245,166,35,0.1); border: 1px solid rgba(245,166,35,0.28); color: #f5a623; }
.hint.ok-hint { background: rgba(67,233,123,0.08); border: 1px solid rgba(67,233,123,0.22); color: #43e97b; margin-top: 0; margin-bottom: 14px; }
.hint code { background: rgba(255,255,255,0.08); padding: 1px 5px; border-radius: 4px; }
.hint pre { margin: 8px 0 0; padding: 8px 10px; background: rgba(0,0,0,0.3); border-radius: 6px; color: rgba(255,255,255,0.75); font-family: Consolas, monospace; font-size: 12px; }

.actions { display: flex; gap: 10px; margin-top: 16px; }
.btn { padding: 8px 16px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.btn-primary { background: rgba(102,126,234,0.25); color: #a8b4ff; }
.btn-primary:hover:not(:disabled) { background: rgba(102,126,234,0.42); }
.btn-ghost { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.6); }
.btn-ghost:hover:not(:disabled) { background: rgba(255,255,255,0.12); }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }

.running { text-align: center; padding: 40px 0; color: rgba(255,255,255,0.5); font-size: 13px; }
.spinner { width: 30px; height: 30px; margin: 0 auto 14px; border: 3px solid rgba(102,126,234,0.25); border-top-color: #667eea; border-radius: 50%; animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.result { margin-top: 4px; }
.stats { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.stat { flex: 1; min-width: 92px; background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 10px; padding: 14px 16px; display: flex; flex-direction: column; gap: 4px; }
.stat b { font-size: 22px; color: rgba(255,255,255,0.9); }
.stat b.ok { color: #43e97b; }
.stat b.bad { color: #f5576c; }
.stat b.warn { color: #f5a623; }
.stat span { font-size: 12px; color: rgba(255,255,255,0.4); }

.meta { font-size: 12px; color: rgba(255,255,255,0.4); margin-bottom: 12px; }
.meta code { background: rgba(255,255,255,0.08); padding: 1px 5px; border-radius: 4px; }
.dry-tag { color: #f5a623; margin-left: 6px; }

.data-card { background: rgba(26,26,46,0.6); border: 1px solid rgba(255,255,255,0.06); border-radius: 12px; overflow: hidden; margin-bottom: 14px; }
.card-title { padding: 12px 16px; font-size: 13px; color: rgba(255,255,255,0.6); border-bottom: 1px solid rgba(255,255,255,0.06); }
.table-scroll { max-height: 380px; overflow: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { position: sticky; top: 0; background: #1a1a2e; padding: 10px 12px; text-align: left; font-size: 12px; color: rgba(255,255,255,0.45); border-bottom: 1px solid rgba(255,255,255,0.08); z-index: 1; }
.data-table td { padding: 8px 12px; font-size: 12px; color: rgba(255,255,255,0.7); border-bottom: 1px solid rgba(255,255,255,0.04); }
.row-add td { background: rgba(67,233,123,0.04); }
.row-update td { background: rgba(79,172,254,0.04); }
.row-fail td { background: rgba(245,87,108,0.06); }
</style>
