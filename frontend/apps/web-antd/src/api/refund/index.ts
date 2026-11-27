import type { PageResult, RefundOrder, RuleConfigMap, ShopItem } from './types';

import { requestClient } from '#/api/request';
import { useAccessStore } from '@vben/stores';
import { useAppConfig } from '@vben/hooks';

/** 退款列表查询参数 */
export interface RefundListParams {
  page?: number;
  pageSize?: number;
  shopId?: number;
  todoStatus?: string;
  ruleResult?: string;
  buyerNick?: string;
  tid?: string;
}

/** 待办统计 */
export interface RefundStats {
  openTotal: number;
  suggestRefund: number;
  suggestConfirm: number;
  needManual: number;
  noLogistics: number;
  doneToday: number;
}

/** 分页查询退款待办 */
export function getRefundListApi(params: RefundListParams) {
  return requestClient.get<PageResult<RefundOrder>>('/refund/list', { params });
}

/** 待办统计 */
export function getRefundStatsApi() {
  return requestClient.get<RefundStats>('/refund/stats');
}

/** 退款详情 */
export function getRefundDetailApi(id: number | string) {
  return requestClient.get<RefundOrder>(`/refund/${id}`);
}

/** 人工处理标记 */
export function handleRefundApi(
  id: number | string,
  data: { remark?: string; todoStatus: string },
) {
  return requestClient.post(`/refund/${id}/handle`, data);
}

/** 批量处理 */
export function batchHandleRefundApi(data: {
  ids: Array<number | string>;
  remark?: string;
  todoStatus: string;
}) {
  return requestClient.post<{ count: number }>('/refund/batch-handle', data);
}

/** 手动触发同步 */
export function syncRefundApi() {
  return requestClient.post<{ count: number }>('/refund/sync');
}

/**
 * 导出 CSV（带鉴权，走浏览器下载）。
 * 不走 JSON 拦截器，直接拉取文件流。
 */
export async function exportRefundCsv(params: RefundListParams) {
  const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);
  const accessStore = useAccessStore();
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const url = `${apiURL}/refund/export?${search.toString()}`;
  const resp = await fetch(url, {
    headers: {
      Authorization: accessStore.accessToken
        ? `Bearer ${accessStore.accessToken}`
        : '',
    },
  });
  if (!resp.ok) {
    throw new Error('导出失败');
  }
  const blob = await resp.blob();
  const link = document.createElement('a');
  const objectUrl = URL.createObjectURL(blob);
  link.href = objectUrl;
  link.download = `退款待办_${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(objectUrl);
}

/** 规则配置 */
export function getRuleConfigApi() {
  return requestClient.get<RuleConfigMap>('/rule/config');
}

/** 更新规则配置 */
export function updateRuleConfigApi(key: string, value: string) {
  return requestClient.put('/rule/config', { key, value });
}

/** 店铺列表 */
export function getShopListApi() {
  return requestClient.get<ShopItem[]>('/shop/list');
}

/** 通知/同步日志 */
export function getNotifyLogsApi(page = 1, pageSize = 20) {
  return requestClient.get<PageResult<Record<string, any>>>('/notify/logs', {
    params: { page, pageSize },
  });
}
