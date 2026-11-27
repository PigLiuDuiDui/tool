<script lang="ts" setup>
import type { RefundOrder } from '#/api/refund/types';
import type { RefundStats } from '#/api/refund';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Empty,
  Form,
  FormItem,
  Input,
  Modal,
  Select,
  Space,
  Tag,
  message,
} from 'ant-design-vue';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  batchHandleRefundApi,
  exportRefundCsv,
  getRefundListApi,
  getRefundStatsApi,
  handleRefundApi,
  syncRefundApi,
} from '#/api/refund';

import { RULE_LABEL, ruleLabel, todoLabel } from '../shared';
import { useColumns } from './data';

defineOptions({ name: 'RefundTodo' });

const router = useRouter();
const syncing = ref(false);
const exporting = ref(false);
const batching = ref(false);
/** 默认收起次要条件，筛选条更矮 */
const filterCollapsed = ref(true);
const listTotal = ref(0);
/** 当前选中的统计卡片：OPEN | 规则码 | DONE_TODAY */
const activeStatKey = ref<string>('OPEN');

const stats = ref<RefundStats>({
  openTotal: 0,
  suggestRefund: 0,
  suggestConfirm: 0,
  needManual: 0,
  noLogistics: 0,
  doneToday: 0,
});

const queryForm = reactive({
  todoStatus: 'OPEN' as string | undefined,
  ruleResult: undefined as string | undefined,
  buyerNick: '',
  tid: '',
});

const ruleOptions = [
  { label: '全部', value: undefined },
  ...Object.entries(RULE_LABEL).map(([value, label]) => ({ label, value })),
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待处理', value: 'OPEN' },
  { label: '已完成', value: 'DONE' },
  { label: '已忽略', value: 'IGNORED' },
];

const overviewItems = computed(() => [
  {
    key: 'OPEN',
    title: '待处理',
    value: stats.value.openTotal,
    icon: 'lucide:inbox',
    tone: 'blue',
  },
  {
    key: 'SUGGEST_REFUND',
    title: '建议退款',
    value: stats.value.suggestRefund,
    icon: 'lucide:badge-percent',
    tone: 'orange',
  },
  {
    key: 'SUGGEST_CONFIRM',
    title: '建议确认收货',
    value: stats.value.suggestConfirm,
    icon: 'lucide:package-check',
    tone: 'cyan',
  },
  {
    key: 'NEED_MANUAL',
    title: '需人工审核',
    value: stats.value.needManual,
    icon: 'lucide:shield-alert',
    tone: 'red',
  },
  {
    key: 'NO_LOGISTICS',
    title: '无物流',
    value: stats.value.noLogistics,
    icon: 'lucide:truck',
    tone: 'gray',
  },
  {
    key: 'DONE_TODAY',
    title: '今日已处理',
    value: stats.value.doneToday,
    icon: 'lucide:circle-check',
    tone: 'green',
  },
]);

const [Grid, gridApi] = useVbenVxeGrid({
  gridClass: 'refund-todo-grid',
  gridOptions: {
    columns: useColumns(),
    // 跟随列表容器剩余高度，避免固定列裁切
    height: '100%',
    keepSource: true,
    border: 'inner',
    stripe: false,
    showOverflow: true,
    emptyText: '暂无退款待办数据',
    rowConfig: {
      keyField: 'id',
      isHover: true,
      height: 52,
    },
    columnConfig: {
      resizable: true,
    },
    checkboxConfig: {
      highlight: true,
      reserve: false,
    },
    scrollX: {
      enabled: true,
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const res = await getRefundListApi({
            page: page.currentPage,
            pageSize: page.pageSize,
            todoStatus: queryForm.todoStatus,
            ruleResult: queryForm.ruleResult,
            buyerNick: queryForm.buyerNick || undefined,
            tid: queryForm.tid || undefined,
          });
          listTotal.value = res.total ?? 0;
          return res;
        },
      },
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: false,
      zoom: true,
    },
  } as VxeTableGridOptions<RefundOrder>,
});

async function loadStats() {
  stats.value = await getRefundStatsApi();
}

async function refreshAll() {
  await loadStats();
  await gridApi.query();
}

/** 根据筛选值回写统计卡片选中态 */
function syncActiveFromQuery() {
  if (queryForm.todoStatus === 'DONE' && !queryForm.ruleResult) {
    activeStatKey.value = 'DONE_TODAY';
    return;
  }
  if (queryForm.todoStatus === 'OPEN' && queryForm.ruleResult) {
    activeStatKey.value = queryForm.ruleResult;
    return;
  }
  if (queryForm.todoStatus === 'OPEN' && !queryForm.ruleResult) {
    activeStatKey.value = 'OPEN';
    return;
  }
  activeStatKey.value = '';
}

async function filterByStat(key: string) {
  activeStatKey.value = key;
  if (key === 'OPEN') {
    queryForm.todoStatus = 'OPEN';
    queryForm.ruleResult = undefined;
  } else if (key === 'DONE_TODAY') {
    queryForm.todoStatus = 'DONE';
    queryForm.ruleResult = undefined;
  } else {
    queryForm.todoStatus = 'OPEN';
    queryForm.ruleResult = key;
  }
  await gridApi.query();
}

async function handleSearch() {
  syncActiveFromQuery();
  await gridApi.query();
}

async function handleReset() {
  queryForm.todoStatus = 'OPEN';
  queryForm.ruleResult = undefined;
  queryForm.buyerNick = '';
  queryForm.tid = '';
  activeStatKey.value = 'OPEN';
  await gridApi.query();
}

async function handleSync() {
  syncing.value = true;
  try {
    const res = await syncRefundApi();
    message.success(`同步完成，处理 ${res.count} 条`);
    await refreshAll();
  } finally {
    syncing.value = false;
  }
}

async function handleExport() {
  exporting.value = true;
  try {
    await exportRefundCsv({
      todoStatus: queryForm.todoStatus,
      ruleResult: queryForm.ruleResult,
      buyerNick: queryForm.buyerNick || undefined,
      tid: queryForm.tid || undefined,
    });
    message.success('已导出 CSV，可用 Excel 打开');
  } catch {
    message.error('导出失败');
  } finally {
    exporting.value = false;
  }
}

function getSelectedIds() {
  const rows = gridApi.grid?.getCheckboxRecords?.() || [];
  return rows.map((row: RefundOrder) => row.id);
}

function confirmBatch(todoStatus: string) {
  const ids = getSelectedIds();
  if (!ids.length) {
    message.warning('请先勾选退款单');
    return;
  }
  Modal.confirm({
    title: todoStatus === 'DONE' ? '批量标记完成' : '批量忽略',
    content: `确认对选中的 ${ids.length} 条执行处理？`,
    async onOk() {
      batching.value = true;
      try {
        const res = await batchHandleRefundApi({
          ids,
          todoStatus,
          remark: '批量处理',
        });
        message.success(`已处理 ${res.count} 条`);
        await refreshAll();
      } finally {
        batching.value = false;
      }
    },
  });
}

function goDetail(row: RefundOrder) {
  router.push(`/refund/detail/${row.id}`);
}

async function quickHandle(row: RefundOrder, todoStatus: string) {
  await handleRefundApi(row.id, {
    todoStatus,
    remark: todoStatus === 'DONE' ? '列表快捷标记完成' : '列表快捷忽略',
  });
  message.success(todoStatus === 'DONE' ? '已标记完成' : '已忽略');
  await refreshAll();
}

onMounted(async () => {
  await loadStats();
});
</script>

<template>
  <Page
    auto-content-height
    class="refund-todo-page"
    content-class="refund-todo-content"
  >
    <template #title>
      <div class="page-heading__title">退款待办</div>
    </template>

    <div class="todo-layout">
      <!-- 统计卡片 -->
      <div class="stat-grid">
        <button
          v-for="item in overviewItems"
          :key="item.key"
          type="button"
          class="stat-card"
          :class="[`tone-${item.tone}`, { 'is-active': activeStatKey === item.key }]"
          @click="filterByStat(item.key)"
        >
          <div class="stat-card__body">
            <div class="stat-card__title">{{ item.title }}</div>
            <div class="stat-card__value">{{ item.value }}</div>
          </div>
          <div class="stat-card__icon">
            <IconifyIcon :icon="item.icon" class="size-5" />
          </div>
        </button>
      </div>

      <!-- 筛选面板 -->
      <section class="panel filter-panel">
        <Form class="filter-bar" layout="inline" @submit.prevent="handleSearch">
          <FormItem label="状态">
            <Select
              v-model:value="queryForm.todoStatus"
              allow-clear
              style="width: 120px"
              :options="statusOptions"
              placeholder="全部"
              @change="syncActiveFromQuery"
            />
          </FormItem>
          <FormItem label="分流">
            <Select
              v-model:value="queryForm.ruleResult"
              allow-clear
              style="width: 140px"
              :options="ruleOptions"
              placeholder="全部"
              @change="syncActiveFromQuery"
            />
          </FormItem>
          <FormItem label="买家">
            <Input
              v-model:value="queryForm.buyerNick"
              allow-clear
              style="width: 140px"
              placeholder="昵称"
            />
          </FormItem>
          <FormItem v-show="!filterCollapsed" label="订单号">
            <Input
              v-model:value="queryForm.tid"
              allow-clear
              style="width: 160px"
              placeholder="订单号"
            />
          </FormItem>
          <FormItem class="filter-actions">
            <Space :size="8">
              <Button @click="handleReset">重置</Button>
              <Button type="primary" html-type="submit">搜索</Button>
              <Button type="link" class="collapse-btn" @click="filterCollapsed = !filterCollapsed">
                {{ filterCollapsed ? '展开' : '收起' }}
                <IconifyIcon
                  :icon="filterCollapsed ? 'lucide:chevron-down' : 'lucide:chevron-up'"
                  class="ml-1 size-4"
                />
              </Button>
            </Space>
          </FormItem>
        </Form>
      </section>

      <!-- 列表面板：占满剩余高度 -->
      <section class="panel list-panel">
        <Grid class="list-grid">
          <template #table-title>
            <div class="list-title">
              <span>退款列表</span>
              <span class="list-total">共 {{ listTotal }} 条</span>
            </div>
          </template>

          <template #toolbar-tools>
            <Space wrap :size="8">
              <Button :loading="syncing" @click="handleSync">
                <template #icon>
                  <IconifyIcon icon="lucide:refresh-cw" class="size-3.5" />
                </template>
                同步退款
              </Button>
              <Button :loading="exporting" @click="handleExport">
                <template #icon>
                  <IconifyIcon icon="lucide:download" class="size-3.5" />
                </template>
                导出 Excel
              </Button>
              <Button type="primary" :loading="batching" @click="confirmBatch('DONE')">
                批量完成
              </Button>
              <Button danger ghost :loading="batching" @click="confirmBatch('IGNORED')">
                批量忽略
              </Button>
            </Space>
          </template>

          <template #empty>
            <div class="empty-box">
              <Empty description="暂无数据">
                <template #image>
                  <IconifyIcon icon="lucide:inbox" class="empty-icon" />
                </template>
              </Empty>
            </div>
          </template>

          <template #refundId="{ row }">
            <a class="link-text" @click="goDetail(row)">{{ row.refundId }}</a>
          </template>

          <template #tid="{ row }">
            <a class="link-text" @click="goDetail(row)">{{ row.tid }}</a>
          </template>

          <template #ruleResult="{ row }">
            <Tag class="soft-tag" :bordered="false">
              {{ ruleLabel(row.ruleResult) }}
            </Tag>
          </template>

          <template #todoStatus="{ row }">
            <Tag
              class="soft-tag"
              :bordered="false"
              :color="
                row.todoStatus === 'OPEN'
                  ? 'processing'
                  : row.todoStatus === 'DONE'
                    ? 'success'
                    : 'default'
              "
            >
              {{ todoLabel(row.todoStatus) }}
            </Tag>
          </template>

          <template #action="{ row }">
            <VbenTableAction
              :actions="[
                {
                  text: '详情',
                  onClick: () => goDetail(row),
                },
                ...(row.todoStatus === 'OPEN'
                  ? [
                      {
                        text: '完成',
                        onClick: () => quickHandle(row, 'DONE'),
                      },
                      {
                        text: '忽略',
                        danger: true,
                        onClick: () => quickHandle(row, 'IGNORED'),
                      },
                    ]
                  : [
                      {
                        text: '重新打开',
                        onClick: () => quickHandle(row, 'OPEN'),
                      },
                    ]),
              ]"
            />
          </template>
        </Grid>
      </section>
    </div>
  </Page>
</template>

<style scoped>
/* 颜色全部跟随 Vben 主题变量，适配亮色/暗色 */
.refund-todo-content {
  display: flex !important;
  flex: 1 1 auto !important;
  flex-direction: column;
  box-sizing: border-box;
  height: 100% !important;
  min-height: 0 !important;
  padding-top: 16px !important;
  overflow: hidden !important;
}

.todo-layout {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 12px;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.page-heading__title {
  color: hsl(var(--foreground));
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 76px;
  padding: 12px 14px;
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  background: hsl(var(--card));
  color: hsl(var(--foreground));
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.stat-card:hover {
  border-color: hsl(var(--primary) / 45%);
}

.stat-card.is-active {
  border-color: hsl(var(--primary));
  background: hsl(var(--primary) / 10%);
  box-shadow: inset 3px 0 0 hsl(var(--primary));
}

.stat-card__title {
  margin-bottom: 8px;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
  line-height: 1.2;
}

.stat-card__value {
  color: hsl(var(--foreground));
  font-size: 22px;
  font-weight: 650;
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.stat-card__icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  flex-shrink: 0;
}

.tone-blue .stat-card__icon {
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 12%);
}

.tone-orange .stat-card__icon {
  color: hsl(var(--warning));
  background: hsl(var(--warning) / 12%);
}

.tone-cyan .stat-card__icon {
  color: hsl(var(--primary));
  background: hsl(var(--accent) / 55%);
}

.tone-red .stat-card__icon {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 12%);
}

.tone-gray .stat-card__icon {
  color: hsl(var(--muted-foreground));
  background: hsl(var(--muted));
}

.tone-green .stat-card__icon {
  color: hsl(var(--success));
  background: hsl(var(--success) / 12%);
}

.panel {
  padding: 12px 16px;
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  background: hsl(var(--card));
}

.filter-panel {
  margin-top: 12px;
  padding-top: 10px;
  padding-bottom: 2px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 4px;
}

.filter-bar :deep(.ant-form-item) {
  margin-inline-end: 12px;
  margin-bottom: 8px;
}

.filter-bar :deep(.ant-form-item-label > label) {
  color: hsl(var(--foreground));
  font-size: 13px;
}

.filter-bar :deep(.ant-select-selector),
.filter-bar :deep(.ant-input-affix-wrapper),
.filter-bar :deep(.ant-input:not(textarea)) {
  height: 32px !important;
  min-height: 32px !important;
  padding-block: 0 !important;
  border-radius: 6px !important;
  line-height: 30px !important;
}

.filter-bar :deep(.ant-input-affix-wrapper .ant-input) {
  height: 100% !important;
  min-height: 0 !important;
  padding: 0 !important;
  line-height: 30px !important;
}

.filter-bar :deep(.ant-select-selection-item),
.filter-bar :deep(.ant-select-selection-placeholder) {
  line-height: 30px !important;
}

.filter-actions {
  margin-inline-end: 0 !important;
  margin-left: auto;
}

.collapse-btn {
  padding-inline: 4px;
}

.list-panel {
  display: flex;
  /* height:0 + flex:1 强制吃掉剩余高度，避免被压成 0 */
  flex: 1 1 auto;
  flex-direction: column;
  box-sizing: border-box;
  width: 100%;
  height: 0;
  min-width: 0;
  min-height: 360px;
  overflow: hidden;
  padding-top: 10px;
  padding-bottom: 8px;
}

.list-panel :deep(.list-grid),
.list-panel :deep(.refund-todo-grid),
.list-panel :deep(.vxe-grid) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  box-sizing: border-box;
  width: 100%;
  height: 100% !important;
  min-width: 0;
  min-height: 0;
  background: transparent;
  color: hsl(var(--foreground));
}

.list-panel :deep(.vxe-grid--layout-wrapper),
.list-panel :deep(.vxe-grid--layout-body-wrapper),
.list-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
  height: auto !important;
}

.list-panel :deep(.vxe-grid--toolbar-wrapper),
.list-panel :deep(.vxe-toolbar) {
  flex-shrink: 0;
  padding: 0 0 12px;
}

.list-panel :deep(.vxe-grid--table-container),
.list-panel :deep(.vxe-table--render-wrapper),
.list-panel :deep(.vxe-table) {
  width: 100% !important;
  min-width: 0;
}

.list-panel :deep(.vxe-table--body-wrapper),
.list-panel :deep(.vxe-table--header-wrapper) {
  width: 100% !important;
}

.list-panel :deep(.vxe-table--fixed-left-wrapper),
.list-panel :deep(.vxe-table--fixed-right-wrapper) {
  z-index: 3;
  background: hsl(var(--card));
}

.list-panel :deep(.vxe-table--fixed-right-wrapper) {
  box-shadow: -6px 0 10px -8px hsl(var(--foreground) / 18%);
}

.list-panel :deep(.vxe-header--column) {
  background: hsl(var(--muted)) !important;
  color: hsl(var(--foreground));
  font-size: 13px;
  font-weight: 600;
}

.list-panel :deep(.vxe-body--row) {
  background: hsl(var(--card));
  color: hsl(var(--foreground));
  font-size: 14px;
}

.list-panel :deep(.vxe-body--row:hover),
.list-panel :deep(.vxe-body--row.row--hover) {
  background: hsl(var(--accent)) !important;
}

.list-panel :deep(.vxe-table--border-line),
.list-panel :deep(.vxe-table--header-border-line) {
  border-color: hsl(var(--border));
}

.list-panel :deep(.vxe-body--column),
.list-panel :deep(.vxe-header--column) {
  border-bottom-color: hsl(var(--border)) !important;
}

.list-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  color: hsl(var(--foreground));
  font-size: 16px;
  font-weight: 600;
}

.list-total {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
  font-weight: 400;
}

.link-text {
  color: hsl(var(--primary));
  cursor: pointer;
}

.link-text:hover {
  text-decoration: underline;
}

.soft-tag {
  margin: 0;
  border-radius: 4px;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  padding: 24px 0;
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: hsl(var(--muted-foreground));
}

@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-actions {
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
