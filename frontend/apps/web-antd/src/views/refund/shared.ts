/** 分流结果中文 */
export const RULE_LABEL: Record<string, string> = {
  PENDING: '待分流',
  SUGGEST_REFUND: '建议退款',
  SUGGEST_CONFIRM: '建议确认收货',
  NEED_MANUAL: '需人工审核',
  NO_LOGISTICS: '无物流',
  OTHER: '其他',
};

/** 分流结果颜色 */
export const RULE_COLOR: Record<string, string> = {
  PENDING: 'default',
  SUGGEST_REFUND: 'success',
  SUGGEST_CONFIRM: 'processing',
  NEED_MANUAL: 'warning',
  NO_LOGISTICS: 'error',
  OTHER: 'default',
};

/** 待办状态中文 */
export const TODO_LABEL: Record<string, string> = {
  OPEN: '待处理',
  DONE: '已完成',
  IGNORED: '已忽略',
};

/** 待办状态颜色 */
export const TODO_COLOR: Record<string, string> = {
  OPEN: 'orange',
  DONE: 'green',
  IGNORED: 'default',
};

export function ruleLabel(code?: string) {
  return (code && RULE_LABEL[code]) || code || '-';
}

export function todoLabel(code?: string) {
  return (code && TODO_LABEL[code]) || code || '-';
}
