<script lang="ts" setup>
import type { ShopItem } from '#/api/refund/types';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Card, Table, Tag } from 'ant-design-vue';

import { getShopListApi } from '#/api/refund';

defineOptions({ name: 'ShopManage' });

const loading = ref(false);
const dataSource = ref<ShopItem[]>([]);

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '店铺名称', dataIndex: 'shopName' },
  { title: '平台店铺 ID', dataIndex: 'platformShopId', width: 160 },
  { title: 'Session', dataIndex: 'sessionCipher', width: 120 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
];

async function load() {
  loading.value = true;
  try {
    dataSource.value = await getShopListApi();
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="店铺授权">
    <Card :bordered="false">
      <Table
        size="middle"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <Tag :color="record.status === 1 ? 'success' : 'default'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'sessionCipher'">
            <Tag>{{ record.sessionCipher ? '已配置' : '未配置' }}</Tag>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
