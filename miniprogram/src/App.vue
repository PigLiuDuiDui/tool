<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { ensureLogin } from './utils/auth'

onLaunch(async () => {
  try {
    await ensureLogin()
  } catch (e) {
    console.warn('登录失败', e)
  }
})
</script>

<style lang="scss">

page {
  background: $bg-page;
  color: $text-primary;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  font-size: 28rpx;
}
</style>


