<script lang="ts" setup>
import type { RefundOrder } from '#/api/refund/types';

import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Form,
  FormItem,
  Input,
  Row,
  Select,
  Space,
  Tag,
  message,
} from 'ant-design-vue';

import { getRefundDetailApi, handleRefundApi } from '#/api/refund';

import { RULE_COLOR, TODO_COLOR, ruleLabel, todoLabel } from '../shared';

defineOptions({ name: 'RefundDetail' });

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const detail = ref<null | RefundOrder>(null);
const todoStatus = ref('DONE');
const remark = ref('');

async function loadDetail() {
  loading.value = true;
  try {
    detail.value = await getRefundDetailApi(String(route.params.id));
    todoStatus.value =
      detail.value.todoStatus === 'OPEN' ? 'DONE' : detail.value.todoStatus;
    remark.value = detail.value.handleRemark || '';
  } finally {
    loading.value = false;
  }
}

async function submitHandle() {
  if (!detail.value) return;
  saving.value = true;
  try {
    await handleRefundApi(detail.value.id, {
      todoStatus: todoStatus.value,
      remark: remark.value,
    });
    message.success('已保存');
    await loadDetail();
  } finally {
    saving.value = false;
  }
}

onMounted(loadDetail);
</script>

<template>
  <Page auto-content-height title="退款详情">
    <template #extra>
      <Space>
        <Button @click="router.push('/refund/todo')">返回待办</Button>
        <Button type="primary" :loading="saving" @click="submitHandle">
          保存处理
        </Button>
      </Space>
    </template>

    <div v-if="detail" class="detail-wrap">
      <Row :gutter="16">
        <Col :xs="24" :lg="16">
          <Card :loading="loading" title="订单信息" class="mb-4">
            <Descriptions bordered :column="2" size="middle">
              <DescriptionsItem label="退款号">
                {{ detail.refundId }}
              </DescriptionsItem>
              <DescriptionsItem label="订单号">{{ detail.tid }}</DescriptionsItem>
              <DescriptionsItem label="买家">
                {{ detail.buyerNick }}
              </DescriptionsItem>
              <DescriptionsItem label="金额">
                ¥ {{ Number(detail.refundFee).toFixed(2) }}
              </DescriptionsItem>
              <DescriptionsItem label="商品" :span="2">
                {{ detail.title }}
              </DescriptionsItem>
              <DescriptionsItem label="退款原因" :span="2">
                {{ detail.reason || '-' }}
              </DescriptionsItem>
              <DescriptionsItem label="货物状态">
                {{ detail.goodStatus || '-' }}
              </DescriptionsItem>
              <DescriptionsItem label="需退货">
                {{ detail.hasGoodReturn ? '是' : '否' }}
              </DescriptionsItem>
              <DescriptionsItem label="物流公司">
                {{ detail.companyName || '-' }}
              </DescriptionsItem>
              <DescriptionsItem label="物流单号">
                {{ detail.sid || '-' }}
              </DescriptionsItem>
              <DescriptionsItem label="退货地址" :span="2">
                {{ detail.returnAddress || '-' }}
              </DescriptionsItem>
              <DescriptionsItem label="物流地址" :span="2">
                {{ detail.logisticsAddress || '-' }}
              </DescriptionsItem>
              <DescriptionsItem label="规则说明" :span="2">
                {{ detail.ruleMessage || '-' }}
              </DescriptionsItem>
            </Descriptions>
          </Card>
        </Col>

        <Col :xs="24" :lg="8">
          <Card title="处理面板" class="mb-4">
            <div class="mb-3 flex items-center gap-2">
              <span class="label">分流</span>
              <Tag :color="RULE_COLOR[detail.ruleResult] || 'default'">
                {{ ruleLabel(detail.ruleResult) }}
              </Tag>
            </div>
            <div class="mb-4 flex items-center gap-2">
              <span class="label">状态</span>
              <Tag :color="TODO_COLOR[detail.todoStatus] || 'default'">
                {{ todoLabel(detail.todoStatus) }}
              </Tag>
            </div>

            <Form layout="vertical">
              <FormItem label="处理结果">
                <Select
                  v-model:value="todoStatus"
                  :options="[
                    { label: '标记完成', value: 'DONE' },
                    { label: '忽略', value: 'IGNORED' },
                    { label: '重新打开', value: 'OPEN' },
                  ]"
                />
              </FormItem>
              <FormItem label="备注">
                <Input.TextArea
                  v-model:value="remark"
                  :rows="4"
                  placeholder="请输入备注"
                />
              </FormItem>
              <Button
                block
                type="primary"
                :loading="saving"
                @click="submitHandle"
              >
                提交处理结果
              </Button>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  </Page>
</template>

<style scoped>
.detail-wrap {
  max-width: 1200px;
}

.label {
  color: hsl(var(--muted-foreground));
  min-width: 36px;
}
</style>
