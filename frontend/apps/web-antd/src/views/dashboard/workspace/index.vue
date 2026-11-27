<script lang="ts" setup>
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';

import { Button, Card, Col, Row, Statistic } from 'ant-design-vue';

defineOptions({ name: 'Workspace' });

const router = useRouter();
</script>

<template>
  <Page title="工作台">
    <Row :gutter="16">
      <Col :span="8">
        <Card>
          <Statistic title="能力范围" value="只读筛查 + 人工处理" />
        </Card>
      </Col>
      <Col :span="8">
        <Card>
          <Statistic title="数据源" value="Mock / 淘宝开放平台" />
        </Card>
      </Col>
      <Col :span="8">
        <Card>
          <Statistic title="通知" value="日志 / Webhook" />
        </Card>
      </Col>
    </Row>
    <Card class="mt-4" title="快捷入口">
      <Button type="primary" class="mr-2" @click="router.push('/refund/todo')">
        进入待办
      </Button>
      <Button class="mr-2" @click="router.push('/refund/rule')">规则配置</Button>
      <Button @click="router.push('/refund/shop')">店铺授权说明</Button>
    </Card>
  </Page>
</template>
