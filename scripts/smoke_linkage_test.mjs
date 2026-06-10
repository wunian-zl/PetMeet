#!/usr/bin/env node

/**
 * PetMeet user/admin linkage smoke test (API-level).
 *
 * Covers:
 * 1) user login
 * 2) admin login
 * 3) user order create/pay
 * 4) admin ship
 * 5) user confirm/review
 * 6) user publish note (with purchased product)
 * 7) admin approve note
 * 8) user verify approved note and order notification
 *
 * Usage:
 *   node scripts/smoke_linkage_test.mjs
 *
 * Optional env:
 *   PETMEET_BASE_URL=http://localhost:8080
 *   PETMEET_ADMIN_USERNAME=admin
 *   PETMEET_ADMIN_PASSWORD=<your-admin-password>
 *   PETMEET_USER_USERNAME=autotest_user_xxx
 *   PETMEET_USER_PASSWORD=PetMeetTest2026
 *   PETMEET_KEEP_CART=false
 *   PETMEET_ALLOW_MUTATION=true
 */

const BASE_URL = process.env.PETMEET_BASE_URL || 'http://localhost:8080'
const ADMIN_USERNAME = process.env.PETMEET_ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = process.env.PETMEET_ADMIN_PASSWORD
const USER_USERNAME =
  process.env.PETMEET_USER_USERNAME || `auto_u_${Math.floor(Date.now() / 1000).toString(36)}`
const USER_PASSWORD = process.env.PETMEET_USER_PASSWORD || 'PetMeetTest2026'
const KEEP_CART = String(process.env.PETMEET_KEEP_CART || 'false').toLowerCase() === 'true'
const ALLOW_MUTATION = String(process.env.PETMEET_ALLOW_MUTATION || 'false').toLowerCase() === 'true'

const RUN_ID = `${Date.now()}_${Math.floor(Math.random() * 10000)}`
const TRACKING_NO = `AUTO${Date.now()}`

if (!ADMIN_PASSWORD) {
  throw new Error('Missing required environment variable: PETMEET_ADMIN_PASSWORD')
}

const state = {
  user: { username: USER_USERNAME, token: '', userId: null },
  admin: { username: ADMIN_USERNAME, token: '', userId: null },
  addressId: null,
  product: null,
  cartItemId: null,
  orderId: null,
  orderSn: '',
  noteId: null,
}

const steps = []

function addStep(name, detail = '') {
  const line = detail ? `${name} - ${detail}` : name
  steps.push(line)
  console.log(`\n[STEP ${steps.length}] ${line}`)
}

function fail(message, extra = null) {
  console.error('\n[FAILED]', message)
  if (extra) {
    console.error('[DETAIL]', extra)
  }
  console.error('\n[SUMMARY]')
  steps.forEach((s, i) => console.error(`${i + 1}. ${s}`))
  process.exit(1)
}

function ok(message) {
  console.log(`[OK] ${message}`)
}

function ensureMutationAllowed() {
  if (ALLOW_MUTATION) {
    return
  }
  console.error('[BLOCKED] smoke_linkage_test is disabled by default because it writes orders, notes, and auto-registered users.')
  console.error('[BLOCKED] Set PETMEET_ALLOW_MUTATION=true only when PETMEET_BASE_URL points to an isolated test environment.')
  process.exit(1)
}

function buildUrl(path, query) {
  const url = new URL(path, BASE_URL)
  if (query && typeof query === 'object') {
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        url.searchParams.set(k, String(v))
      }
    })
  }
  return url.toString()
}

async function api(path, { method = 'GET', token = '', query, body } = {}) {
  const url = buildUrl(path, query)
  const headers = {}
  if (token) headers.Authorization = token
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  const text = await res.text()
  let json = null
  try {
    json = JSON.parse(text)
  } catch (e) {
    fail(`Non-JSON response from ${method} ${path}`, text)
  }

  if (!res.ok) {
    fail(`HTTP ${res.status} from ${method} ${path}`, json)
  }

  if (json.code !== 200) {
    fail(`Business error from ${method} ${path}`, json)
  }

  return json.data
}

async function waitFor(predicate, { timeoutMs = 10000, intervalMs = 500, label = 'condition' } = {}) {
  const start = Date.now()
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const result = await predicate()
    if (result) return result
    if (Date.now() - start > timeoutMs) {
      fail(`Timeout waiting for ${label}`)
    }
    await new Promise((r) => setTimeout(r, intervalMs))
  }
}

async function loginUser() {
  addStep('User login')
  const data = await api('/auth/login', {
    method: 'POST',
    body: { username: USER_USERNAME, password: USER_PASSWORD },
  })
  state.user.token = data.token
  state.user.userId = data.userId
  ok(`user=${data.username} id=${data.userId}`)
}

async function loginAdmin() {
  addStep('Admin login')
  const data = await api('/admin/auth/login', {
    method: 'POST',
    body: { username: ADMIN_USERNAME, password: ADMIN_PASSWORD },
  })
  state.admin.token = data.token
  state.admin.userId = data.userId
  ok(`admin=${data.username} id=${data.userId}`)
}

async function ensureAddress() {
  addStep('Ensure user address')
  const list = await api('/user/address/list', { token: state.user.token })
  const arr = Array.isArray(list) ? list : []
  if (arr.length > 0) {
    const chosen = arr.find((a) => Number(a?.isDefault) === 1) || arr[0]
    state.addressId = chosen.id
    ok(`reuse addressId=${state.addressId}`)
    return
  }

  const id = await api('/user/address', {
    method: 'POST',
    token: state.user.token,
    body: {
      name: '自动化联调用户',
      phone: '13800138000',
      province: '北京市',
      city: '北京市',
      region: '海淀区',
      detailAddress: `软件园${Math.floor(Math.random() * 99) + 1}号`,
      isDefault: 1,
    },
  })
  state.addressId = id
  ok(`created addressId=${state.addressId}`)
}

async function pickProduct() {
  addStep('Pick in-stock product')
  const page = await api('/product/list', {
    query: { pageNum: 1, pageSize: 50 },
  })
  const records = Array.isArray(page?.records) ? page.records : []
  const product = records.find((p) => Number(p?.stock || 0) > 0)
  if (!product) {
    fail('No in-stock product found from /product/list')
  }
  state.product = product
  ok(`productId=${product.id} name=${product.name}`)
}

async function prepareCartAndSubmitOrder() {
  addStep('Create user order (cart -> submit)')
  if (!KEEP_CART) {
    await api('/cart/clear', { method: 'DELETE', token: state.user.token })
  }

  await api('/cart/add', {
    method: 'POST',
    token: state.user.token,
    body: { productId: state.product.id, quantity: 1 },
  })

  const cart = await api('/cart/list', { token: state.user.token })
  const cartItems = Array.isArray(cart) ? cart : []
  const item = cartItems.find((c) => Number(c.productId) === Number(state.product.id))
  if (!item?.id) {
    fail('Added cart item not found')
  }
  state.cartItemId = item.id

  const orderId = await api('/order/submit', {
    method: 'POST',
    token: state.user.token,
    body: {
      cartItemIds: [state.cartItemId],
      addressId: state.addressId,
      remark: `smoke:${RUN_ID}`,
    },
  })
  state.orderId = orderId
  ok(`orderId=${state.orderId}`)
}

async function payAndShipOrder() {
  addStep('Pay order (user) then ship (admin)')
  await api(`/order/pay/${state.orderId}`, {
    method: 'POST',
    token: state.user.token,
  })

  const userDetailAfterPay = await api(`/order/detail/${state.orderId}`, {
    token: state.user.token,
  })
  if (Number(userDetailAfterPay?.status) !== 1) {
    fail('Order status is not paid(1) after /order/pay', userDetailAfterPay)
  }
  state.orderSn = userDetailAfterPay.orderSn || ''

  await api(`/admin/order/${state.orderId}/ship`, {
    method: 'PUT',
    token: state.admin.token,
    body: { company: '自动化快递', trackingNo: TRACKING_NO },
  })

  const userDetailAfterShip = await waitFor(async () => {
    const d = await api(`/order/detail/${state.orderId}`, { token: state.user.token })
    return Number(d?.status) === 2 ? d : null
  }, { label: 'order status become shipped(2)' })

  ok(`shipped orderSn=${userDetailAfterShip.orderSn}`)
}

async function confirmAndReviewOrder() {
  addStep('Confirm receipt and review (user)')
  await api(`/order/confirm/${state.orderId}`, {
    method: 'POST',
    token: state.user.token,
  })

  const detailAfterConfirm = await waitFor(async () => {
    const d = await api(`/order/detail/${state.orderId}`, { token: state.user.token })
    return Number(d?.status) === 3 ? d : null
  }, { label: 'order status become completed(3)' })

  await api(`/order/review/${state.orderId}`, {
    method: 'POST',
    token: state.user.token,
    body: { score: 5, content: `smoke review ${RUN_ID}` },
  })

  const detailAfterReview = await api(`/order/detail/${state.orderId}`, {
    token: state.user.token,
  })
  if (Number(detailAfterReview?.reviewStatus ?? 0) !== 1) {
    fail('Order reviewStatus is not 1 after review submit', detailAfterReview)
  }
  ok(`completed+reviewed orderId=${detailAfterConfirm.id}`)
}

async function publishAndApproveNote() {
  addStep('Publish note (user) and approve (admin)')
  const noteId = await api('/note/publish', {
    method: 'POST',
    token: state.user.token,
    body: {
      title: `自动化联调笔记_${RUN_ID}`,
      content: `自动化联调内容_${RUN_ID}`,
      category: 'review',
      tags: ['自动化', '联调', '下单回归'],
      coverImg: state.product.coverImg || '',
      images: state.product.coverImg ? [state.product.coverImg] : [],
      type: 'image',
      videoUrl: '',
      productIds: [state.product.id],
    },
  })
  state.noteId = noteId

  const notePending = await api(`/admin/note/${state.noteId}`, {
    token: state.admin.token,
  })
  if (Number(notePending?.status) !== 0) {
    fail('New note status is not pending(0) before admin approve', notePending)
  }

  await api(`/admin/note/${state.noteId}/approve`, {
    method: 'PUT',
    token: state.admin.token,
  })

  const approved = await waitFor(async () => {
    const d = await api(`/note/detail/${state.noteId}`, { token: state.user.token })
    return Number(d?.status) === 1 ? d : null
  }, { label: 'note status become approved(1)' })

  ok(`approved noteId=${approved.id}`)
}

async function verifyNotificationsAndAdminView() {
  addStep('Verify user notification + admin order query')

  const noticePage = await api('/notification/list', {
    token: state.user.token,
    query: { pageNum: 1, pageSize: 30 },
  })
  const notices = Array.isArray(noticePage?.records) ? noticePage.records : []
  const hit = notices.find((n) => Number(n?.bizId) === Number(state.orderId))
  if (!hit) {
    fail('No notification found for current orderId', {
      orderId: state.orderId,
      latestNotifications: notices.slice(0, 5),
    })
  }

  const adminOrderPage = await api('/admin/order/list', {
    token: state.admin.token,
    query: { pageNum: 1, pageSize: 20, orderNo: state.orderSn },
  })
  const records = Array.isArray(adminOrderPage?.records) ? adminOrderPage.records : []
  const orderHit = records.find((o) => Number(o?.id) === Number(state.orderId))
  if (!orderHit) {
    fail('Admin order list cannot query the created order by orderNo', {
      orderId: state.orderId,
      orderSn: state.orderSn,
      records,
    })
  }
  ok(`notification+admin-query ok for orderId=${state.orderId}`)
}

async function run() {
  ensureMutationAllowed()
  console.log(`[INFO] BASE_URL=${BASE_URL}`)
  console.log(`[INFO] USER=${USER_USERNAME}`)
  console.log(`[INFO] ADMIN=${ADMIN_USERNAME}`)

  addStep('Health check')
  const health = await api('/hello/status')
  ok(`backend status=${health?.status || 'unknown'}`)

  await loginUser()
  await loginAdmin()
  await ensureAddress()
  await pickProduct()
  await prepareCartAndSubmitOrder()
  await payAndShipOrder()
  await confirmAndReviewOrder()
  await publishAndApproveNote()
  await verifyNotificationsAndAdminView()

  console.log('\n[PASS] Linkage smoke test passed.')
  console.log('[RESULT]', JSON.stringify({
    runId: RUN_ID,
    baseUrl: BASE_URL,
    user: state.user.username,
    admin: state.admin.username,
    orderId: state.orderId,
    orderSn: state.orderSn,
    noteId: state.noteId,
    productId: state.product?.id,
  }, null, 2))
}

run().catch((e) => {
  fail(e?.message || 'Unexpected error', e?.stack || e)
})
