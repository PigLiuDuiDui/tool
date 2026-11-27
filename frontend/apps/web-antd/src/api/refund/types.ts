/** 分页结果（对齐后端 PageResult） */
export interface PageResult<T> {
  items: T[];
  total: number;
}

/** 退款单 */
export interface RefundOrder {
  id: number;
  shopId: number;
  refundId: string;
  tid: string;
  oid: string;
  buyerNick: string;
  title: string;
  refundFee: number;
  status: string;
  orderStatus: string;
  goodStatus: string;
  hasGoodReturn: boolean;
  sid: string;
  companyName: string;
  returnAddress: string;
  logisticsAddress: string;
  reason: string;
  ruleResult: string;
  ruleMessage: string;
  addressMatched: number;
  todoStatus: string;
  handleRemark: string;
  handledAt?: string;
  syncedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 规则配置 */
export type RuleConfigMap = Record<string, string>;

/** 店铺 */
export interface ShopItem {
  id: number;
  shopName: string;
  platformShopId: string;
  sessionCipher: string;
  status: number;
  remark: string;
}
