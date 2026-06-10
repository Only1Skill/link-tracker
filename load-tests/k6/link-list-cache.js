import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8081';
const CHAT_ID = __ENV.CHAT_ID || '700001';
const LINKS_COUNT = Number(__ENV.LINKS_COUNT || 20);

const LOAD_RATE = Number(__ENV.LOAD_RATE || 50);
const LOAD_DURATION = __ENV.LOAD_DURATION || '1m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 20);
const MAX_VUS = Number(__ENV.MAX_VUS || 100);

const CLEANUP_AFTER_TEST = (__ENV.CLEANUP_AFTER_TEST || 'false') === 'true';

const getLinksDuration = new Trend('get_links_duration');
const getLinksStatusOk = new Rate('get_links_status_ok');
const getLinksBodyOk = new Rate('get_links_body_ok');
const getLinksRequests = new Counter('get_links_requests');

export const options = {
  scenarios: {
    get_links_constant_rate: {
      executor: 'constant-arrival-rate',
      rate: LOAD_RATE,
      timeUnit: '1s',
      duration: LOAD_DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
    get_links_status_ok: ['rate>0.99'],
    get_links_body_ok: ['rate>0.99'],
  },
};

export function setup() {
  const chatId = String(CHAT_ID);

  registerChat(chatId);
  addLinks(chatId);
  warmUpCache(chatId);

  return {
    chatId,
    linksCount: LINKS_COUNT,
  };
}

export default function (data) {
  const response = http.get(`${BASE_URL}/links`, {
    headers: {
      'Tg-Chat-Id': data.chatId,
    },
    tags: {
      endpoint: 'GET /links',
      cache_test: 'link-list',
    },
  });

  getLinksRequests.add(1);
  getLinksDuration.add(response.timings.duration);

  const statusOk = response.status === 200;
  const bodyOk = hasExpectedLinks(response, data.linksCount);

  getLinksStatusOk.add(statusOk);
  getLinksBodyOk.add(bodyOk);

  check(response, {
    'GET /links вернул 200': () => statusOk,
    'GET /links вернул ожидаемый список ссылок': () => bodyOk,
  });
}

export function teardown(data) {
  if (!CLEANUP_AFTER_TEST) {
    return;
  }

  http.del(`${BASE_URL}/tg-chat/${data.chatId}`);
}

function registerChat(chatId) {
  const response = http.post(`${BASE_URL}/tg-chat/${chatId}`);

  check(response, {
    'чат зарегистрирован или уже существует': (res) => res.status === 200 || res.status === 400,
  });
}

function addLinks(chatId) {
  for (let i = 0; i < LINKS_COUNT; i += 1) {
    const requestBody = JSON.stringify({
      link: `https://github.com/load-test-owner/repo-${chatId}-${i}`,
      tags: ['load', 'cache'],
    });

    const response = http.post(`${BASE_URL}/links`, requestBody, {
      headers: {
        'Tg-Chat-Id': chatId,
        'Content-Type': 'application/json',
      },
    });

    check(response, {
      'ссылка добавлена или уже существует': (res) => res.status === 200 || res.status === 400,
    });
  }
}

function warmUpCache(chatId) {
  const response = http.get(`${BASE_URL}/links`, {
    headers: {
      'Tg-Chat-Id': chatId,
    },
  });

  check(response, {
    'прогревочный GET /links вернул 200': (res) => res.status === 200,
    'прогревочный GET /links вернул ссылки': (res) => hasExpectedLinks(res, LINKS_COUNT),
  });
}

function hasExpectedLinks(response, expectedLinksCount) {
  if (response.status !== 200) {
    return false;
  }

  try {
    const body = JSON.parse(response.body);
    return Array.isArray(body) && body.length >= expectedLinksCount;
  } catch (_) {
    return false;
  }
}
