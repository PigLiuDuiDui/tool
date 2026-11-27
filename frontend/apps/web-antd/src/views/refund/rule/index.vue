<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Form,
  FormItem,
  Input,
  InputNumber,
  Space,
  message,
} from 'ant-design-vue';

import { getRuleConfigApi, updateRuleConfigApi } from '#/api/refund';

defineOptions({ name: 'RuleConfig' });

const loading = ref(false);
const saving = ref(false);
const form = reactive({
  amount_threshold: 50,
  return_address_keywords: '',
});

async function load() {
  loading.value = true;
  try {
    const data = await getRuleConfigApi();
    form.amount_threshold = Number(data.amount_threshold || 50);
    form.return_address_keywords = data.return_address_keywords || '';
  } finally {
    loading.value = false;
  }
}

async function save() {
  saving.value = true;
  try {
    await updateRuleConfigApi(
      'amount_threshold',
      String(form.amount_threshold),
    );
    await updateRuleConfigApi(
      'return_address_keywords',
      form.return_address_keywords,
    );
    message.success('已保存');
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="规则配置">
    <Card :loading="loading" title="分流参数" style="max-width: 640px">
      <Form layout="vertical">
        <FormItem label="建议退款金额阈值（元）">
          <InputNumber
            v-model:value="form.amount_threshold"
            :min="0"
            :precision="2"
            style="width: 220px"
          />
        </FormItem>
        <FormItem label="退货地址匹配关键词">
          <Input.TextArea
            v-model:value="form.return_address_keywords"
            :rows="4"
            placeholder="多个关键词用英文逗号分隔"
          />
        </FormItem>
        <Space>
          <Button type="primary" :loading="saving" @click="save">保存</Button>
          <Button @click="load">重新加载</Button>
        </Space>
      </Form>
    </Card>
  </Page>
</template>
