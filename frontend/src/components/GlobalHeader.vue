<template>
  <div id="globalHeader">
    <a-row :wrap="false" align="middle">
      <a-col flex="200px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" alt="logo" />
            <div class="title">像素屿</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <!-- 通知铃铛 + 主题切换 + 用户信息 -->
      <a-col flex="200px">
        <div class="user-area">
          <ThemeToggle />
          <NotificationBell v-if="loginUserStore.loginUser.id" />
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space class="user-trigger">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32" />
                <span class="user-name">{{ loginUserStore.loginUser.userName ?? '无名' }}</span>
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <router-link to="/my_space">
                      <UserOutlined />
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <!-- 普通用户：会员兑换入口 -->
                  <a-menu-item v-if="loginUserStore.loginUser.userRole === 'user'">
                    <router-link to="/user_exchange_vip">
                      <CrownOutlined />
                      会员兑换
                    </router-link>
                  </a-menu-item>
                  <!-- VIP用户：到期时间 + 续费入口 -->
                  <template v-if="loginUserStore.loginUser.userRole === 'vip'">
                    <a-menu-item disabled>
                      <CrownOutlined />
                      会员到期：{{ loginUserStore.loginUser.vipExpireTime?.substring(0, 10) || '未知' }}
                    </a-menu-item>
                    <a-menu-item>
                      <router-link to="/user_exchange_vip">
                        续费会员
                      </router-link>
                    </a-menu-item>
                  </template>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { CrownOutlined, HomeOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import NotificationBell from '@/components/notification/NotificationBell.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const loginUserStore = useLoginUserStore()

// 未经过滤的菜单项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
]

// 根据权限过滤菜单项
const filterMenus = (menus: any[] = []) => {
  return menus?.filter((menu) => {
    if (menu?.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const items = computed(() => filterMenus(originItems))

const router = useRouter()
const current = ref<string[]>([])
router.afterEach((to) => {
  current.value = [to.path]
})

const doMenuClick = ({ key }: { key: string }) => {
  router.push({ path: key })
}

const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
#globalHeader .title-bar {
  display: flex;
  align-items: center;
}

.title {
  color: var(--yu-text-primary);
  font-size: 18px;
  font-weight: 600;
  margin-left: 12px;
}

.logo {
  height: 36px;
}

.user-area {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.user-trigger {
  cursor: pointer;
  padding: 2px 8px;
  border-radius: var(--yu-radius-md);
  transition: background var(--yu-transition-fast);
}

.user-trigger:hover {
  background: var(--yu-bg-hover);
}

.user-name {
  color: var(--yu-text-primary);
  font-size: 14px;
}

@media (max-width: 768px) {
  .title { display: none; }
  .user-name { display: none; }
  #globalHeader :deep(.ant-menu) { font-size: 13px; }
  #globalHeader :deep(.ant-menu-item) { padding: 0 12px; }
}

/* 覆盖 antd menu 水平模式暗色 */
:deep(.ant-menu) {
  background: transparent !important;
  border-bottom: none !important;
  color: var(--yu-text-secondary);
}

:deep(.ant-menu-item) {
  color: var(--yu-text-secondary) !important;
}

:deep(.ant-menu-item:hover),
:deep(.ant-menu-item-active),
:deep(.ant-menu-item-selected) {
  color: var(--yu-text-primary) !important;
}
</style>
