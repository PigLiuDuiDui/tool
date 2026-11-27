import type { VxeTableGridColumns } from '#/adapter/vxe-table';

/**
 * 表格列：尽量用 minWidth，让宽屏自适应拉伸；窄屏横向滚动，操作列固定右侧。
 */
export function useColumns(): VxeTableGridColumns {
  return [
    { type: 'checkbox', width: 48, align: 'center', fixed: 'left' },
    {
      field: 'refundId',
      title: '退款号',
      minWidth: 120,
      slots: { default: 'refundId' },
    },
    {
      field: 'tid',
      title: '订单号',
      minWidth: 140,
      slots: { default: 'tid' },
    },
    {
      field: 'buyerNick',
      title: '买家',
      minWidth: 110,
      showOverflow: 'tooltip',
    },
    {
      field: 'title',
      title: '商品',
      minWidth: 160,
      showOverflow: 'tooltip',
    },
    {
      field: 'refundFee',
      title: '金额(元)',
      minWidth: 96,
      align: 'right',
      formatter: ({ cellValue }) =>
        cellValue == null ? '-' : Number(cellValue).toFixed(2),
    },
    {
      field: 'sid',
      title: '物流',
      minWidth: 140,
      showOverflow: 'tooltip',
      formatter: ({ row }) =>
        row.sid ? `${row.companyName || '快递'} ${row.sid}` : '-',
    },
    {
      field: 'ruleResult',
      title: '分流结果',
      minWidth: 120,
      slots: { default: 'ruleResult' },
    },
    {
      field: 'ruleMessage',
      title: '说明',
      minWidth: 160,
      showOverflow: 'tooltip',
    },
    {
      field: 'todoStatus',
      title: '状态',
      minWidth: 90,
      slots: { default: 'todoStatus' },
    },
    {
      field: 'action',
      fixed: 'right',
      slots: { default: 'action' },
      title: '操作',
      width: 180,
      align: 'center',
    },
  ];
}
