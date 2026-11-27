<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Space,
  Table,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';

import { getNotifyLogsApi, syncRefundApi } from '#/api/refund';

defineOptions({ name: 'SyncLog' });

const loading = ref(false);
const syncing = ref(false);
const dataSource = ref<Record<string, any>[]>([]);
const total = ref(0);
const query = reactive({ page: 1, pageSize: 10 });

const columns = [
  { title: '时间', dataIndex: 'createdAt', width: 180 },
  { title: '类型', dataIndex: 'bizType', width: 140 },
  { title: '业务 ID', dataIndex: 'bizId', width: 140 },
  { title: '渠道', dataIndex: 'channel', width: 100 },
  { title: '结果', dataIndex: 'success', width: 90 },
  { title: '内容', dataIndex: 'content', ellipsis: true },
];

async function load() {
  loading.value = true;
  try {
    const res = await getNotifyLogsApi(query.page, query.pageSize);
    dataSource.value = res.items ?? [];
    total.value = res.total ?? 0;
  } finally {
    loading.value = false;
  }
}

async function handleSync() {
  syncing.value = true;
  try {
    const res = await syncRefundApi();
    message.success(`同步完成，处理 ${res.count} 条`);
    await load();
  } finally {
    syncing.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="同步与通知日志">
    <template #extra>
      <Space>
        <Button @click="load">刷新</Button>
        <Button type="primary" :loading="syncing" @click="handleSync">
          立即同步
        </Button>
      </Space>
    </template>

    <Card :bordered="false">
      <Table
        size="middle"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        row-key="id"
        :pagination="{
          current: query.page,
          pageSize: query.pageSize,
          total,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: (page: number, pageSize: number) => {
            query.page = page;
            query.pageSize = pageSize;
            load();
          },
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'success'">
            <Tag :color="record.success ? 'success' : 'error'">
              {{ record.success ? '成功' : '失败' }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'content'">
            <Tooltip :title="record.content">
              <span>{{ record.content }}</span>
            </Tooltip>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
