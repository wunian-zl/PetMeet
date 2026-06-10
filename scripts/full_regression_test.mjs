#!/usr/bin/env node

/**
 * PetMeet full API regression test (user + admin linkage).
 *
 * This script focuses on cross-module integration at API level:
 * - auth/user profile/address
 * - product/category/cart
 * - order lifecycle (submit/pay/ship/confirm/review)
 * - after-sale flow
 * - refund flow
 * - note publish/audit/interaction/comment/follow/complaint
 * - notification read flow
 * - admin banner/category/user/product CRUD
 * - admin dashboard
 * - upload endpoints
 *
 * Usage:
 *   node scripts/full_regression_test.mjs
 *
 * Optional env:
 *   PETMEET_BASE_URL=http://localhost:8080
 *   PETMEET_ADMIN_USERNAME=admin
 *   PETMEET_ADMIN_PASSWORD=<your-admin-password>
 *   PETMEET_USER_USERNAME=auto_u_xxx
 *   PETMEET_USER_PASSWORD=PetMeetTest2026
 *   PETMEET_ALLOW_MUTATION=true
 */

const BASE_URL = process.env.PETMEET_BASE_URL || "http://localhost:8080";
const ADMIN_USERNAME = process.env.PETMEET_ADMIN_USERNAME || "admin";
const ADMIN_PASSWORD = process.env.PETMEET_ADMIN_PASSWORD;
const USER_PASSWORD = process.env.PETMEET_USER_PASSWORD || "PetMeetTest2026";
const ALLOW_MUTATION = String(process.env.PETMEET_ALLOW_MUTATION || "false").toLowerCase() === "true";
const RUN_TAG = `${Date.now().toString(36)}${Math.floor(Math.random() * 10000).toString(36)}`;
const USER_USERNAME = process.env.PETMEET_USER_USERNAME || `au_${RUN_TAG}`.slice(0, 20);
const PEER_USERNAME = `ap_${RUN_TAG}`.slice(0, 20);
const TRACKING_NO = `AUTO${Date.now()}`;

if (!ADMIN_PASSWORD) {
  throw new Error("Missing required environment variable: PETMEET_ADMIN_PASSWORD");
}

const state = {
  user: { username: USER_USERNAME, token: "", userId: null },
  admin: { username: ADMIN_USERNAME, token: "", userId: null },
  peer: { username: PEER_USERNAME, token: "", userId: null, noteId: null },
  product: null,
  categoryId: null,
  defaultAddressId: null,
  mainOrder: null,
  noteId: null,
  complaintId: null,
  afterSaleOrder: null,
  refundOrder: null,
};

const cleanupTasks = [];
const results = [];

class ApiError extends Error {
  constructor(message, detail) {
    super(message);
    this.name = "ApiError";
    this.detail = detail;
  }
}

function shortJson(v) {
  try {
    const text = JSON.stringify(v);
    return text.length > 500 ? `${text.slice(0, 500)}...` : text;
  } catch {
    return String(v);
  }
}

function assert(condition, message, detail) {
  if (!condition) {
    throw new ApiError(message, detail);
  }
}

function buildUrl(path, query) {
  const url = new URL(path, BASE_URL);
  if (query && typeof query === "object") {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function api(path, { method = "GET", token = "", query, body, formData } = {}) {
  const url = buildUrl(path, query);
  const headers = {};
  if (token) headers.Authorization = token;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  const res = await fetch(url, {
    method,
    headers,
    body: formData || (body !== undefined ? JSON.stringify(body) : undefined),
  });

  const text = await res.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch (e) {
    throw new ApiError(`Non-JSON response: ${method} ${path}`, { status: res.status, text });
  }

  if (!res.ok) {
    throw new ApiError(`HTTP ${res.status}: ${method} ${path}`, json);
  }
  if (json.code !== 200) {
    throw new ApiError(`Business error ${json.code}: ${method} ${path}`, json);
  }
  return json.data;
}

function getRecords(pageData) {
  return Array.isArray(pageData?.records) ? pageData.records : [];
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function ensureMutationAllowed() {
  if (ALLOW_MUTATION) {
    return;
  }
  console.error("[BLOCKED] full_regression_test is disabled by default because it writes users, orders, notes, complaints, and admin test data.");
  console.error("[BLOCKED] Set PETMEET_ALLOW_MUTATION=true only when PETMEET_BASE_URL points to an isolated test environment.");
  process.exit(1);
}

async function waitFor(predicate, { timeoutMs = 12000, intervalMs = 400, label = "condition" } = {}) {
  const start = Date.now();
  while (Date.now() - start <= timeoutMs) {
    const val = await predicate();
    if (val) return val;
    await sleep(intervalMs);
  }
  throw new ApiError(`Timeout waiting for ${label}`);
}

async function runCase(name, fn, { required = false } = {}) {
  const started = Date.now();
  try {
    const detail = await fn();
    results.push({ name, status: "PASS", ms: Date.now() - started, detail: detail || "" });
    console.log(`[PASS] ${name}${detail ? ` - ${detail}` : ""}`);
  } catch (e) {
    const msg = e?.message || String(e);
    const detail = e?.detail !== undefined ? shortJson(e.detail) : "";
    results.push({ name, status: "FAIL", ms: Date.now() - started, detail: `${msg}${detail ? ` | ${detail}` : ""}` });
    console.error(`[FAIL] ${name} - ${msg}`);
    if (detail) {
      console.error(`[DETAIL] ${detail}`);
    }
    if (required) throw e;
  }
}

async function loginUser(username, password, isAdmin = false) {
  const path = isAdmin ? "/admin/auth/login" : "/auth/login";
  const data = await api(path, {
    method: "POST",
    body: { username, password },
  });
  return data;
}

async function ensureDefaultAddress() {
  const list = await api("/user/address/list", { token: state.user.token });
  const addresses = Array.isArray(list) ? list : [];
  if (addresses.length > 0) {
    const target = addresses.find((a) => Number(a?.isDefault) === 1) || addresses[0];
    state.defaultAddressId = target.id;
    return target.id;
  }

  const createdId = await api("/user/address", {
    method: "POST",
    token: state.user.token,
    body: {
      name: "Auto Tester",
      phone: "13800138000",
      province: "TestProvince",
      city: "TestCity",
      region: "TestRegion",
      detailAddress: `Auto Street ${RUN_TAG}`,
      isDefault: 1,
    },
  });
  state.defaultAddressId = createdId;
  return createdId;
}

async function pickProduct() {
  const page = await api("/product/list", { query: { pageNum: 1, pageSize: 100 } });
  const records = getRecords(page);
  assert(records.length > 0, "No product found from /product/list");
  const inStock = records
    .filter((p) => Number(p?.stock || 0) > 3)
    .sort((a, b) => Number(b?.stock || 0) - Number(a?.stock || 0));
  const chosen = inStock[0] || records[0];
  assert(chosen?.id, "Failed to pick product", records.slice(0, 3));
  state.product = chosen;
  state.categoryId = chosen.categoryId || null;
  return chosen;
}

async function createOrderFromCart({ remark }) {
  await api("/cart/clear", { method: "DELETE", token: state.user.token });
  await api("/cart/add", {
    method: "POST",
    token: state.user.token,
    body: { productId: state.product.id, quantity: 1 },
  });

  const cartList = await api("/cart/list", { token: state.user.token });
  const cartItem = (Array.isArray(cartList) ? cartList : []).find((c) => Number(c?.productId) === Number(state.product.id));
  assert(cartItem?.id, "Added cart item missing", cartList);

  const orderId = await api("/order/submit", {
    method: "POST",
    token: state.user.token,
    body: {
      cartItemIds: [cartItem.id],
      addressId: state.defaultAddressId,
      remark,
    },
  });
  const detail = await api(`/order/detail/${orderId}`, { token: state.user.token });
  const firstItemId = detail?.items?.[0]?.id;
  assert(firstItemId, "Order item missing", detail);
  return {
    id: orderId,
    orderSn: detail.orderSn,
    itemId: firstItemId,
  };
}

async function createPeerApprovedNote() {
  const noteId = await api("/note/publish", {
    method: "POST",
    token: state.peer.token,
    body: {
      title: `Peer note ${RUN_TAG}`,
      content: `Peer content ${RUN_TAG}`,
      category: "review",
      tags: ["auto", "peer"],
      coverImg: state.product?.coverImg || "",
      images: state.product?.coverImg ? [state.product.coverImg] : [],
      type: "image",
      videoUrl: "",
      productIds: [state.product.id],
    },
  });
  await api(`/admin/note/${noteId}/approve`, {
    method: "PUT",
    token: state.admin.token,
  });
  await waitFor(async () => {
    const d = await api(`/note/detail/${noteId}`, { token: state.peer.token });
    return Number(d?.status) === 1 ? d : null;
  }, { label: "peer note approved" });
  state.peer.noteId = noteId;
  return noteId;
}

async function safeCleanup() {
  for (let i = cleanupTasks.length - 1; i >= 0; i -= 1) {
    try {
      await cleanupTasks[i]();
    } catch (e) {
      console.error(`[CLEANUP-WARN] ${e?.message || e}`);
    }
  }
}

function makeTinyPngBlob() {
  const base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7+M4sAAAAASUVORK5CYII=";
  const bytes = Buffer.from(base64, "base64");
  return new Blob([bytes], { type: "image/png" });
}

async function main() {
  ensureMutationAllowed();
  console.log(`[INFO] BASE_URL=${BASE_URL}`);
  console.log(`[INFO] USER=${USER_USERNAME}`);
  console.log(`[INFO] ADMIN=${ADMIN_USERNAME}`);

  let setupFailed = false;

  try {
    await runCase("Health check", async () => {
      const data = await api("/hello/status");
      assert(data?.status, "Health status missing", data);
      return `status=${data.status}`;
    }, { required: true });

    await runCase("Login user/admin/peer", async () => {
      const user = await loginUser(state.user.username, USER_PASSWORD, false);
      state.user.token = user.token;
      state.user.userId = user.userId;

      const admin = await loginUser(state.admin.username, ADMIN_PASSWORD, true);
      state.admin.token = admin.token;
      state.admin.userId = admin.userId;

      const peer = await loginUser(state.peer.username, USER_PASSWORD, false);
      state.peer.token = peer.token;
      state.peer.userId = peer.userId;

      assert(state.user.token && state.admin.token && state.peer.token, "Missing tokens after login");
      return `userId=${state.user.userId}, adminId=${state.admin.userId}, peerId=${state.peer.userId}`;
    }, { required: true });

    await runCase("User profile + address CRUD", async () => {
      const info = await api("/user/info", { token: state.user.token });
      assert(info?.id, "User info missing id", info);

      await api("/user/info", {
        method: "PUT",
        token: state.user.token,
        body: { nickname: info.nickname || state.user.username },
      });

      const tempAddressId = await api("/user/address", {
        method: "POST",
        token: state.user.token,
        body: {
          name: "Temp Addr",
          phone: "13900139000",
          province: "P",
          city: "C",
          region: "R",
          detailAddress: `D-${RUN_TAG}`,
          isDefault: 0,
        },
      });
      await api("/user/address", {
        method: "PUT",
        token: state.user.token,
        body: {
          id: tempAddressId,
          name: "Temp Addr 2",
          phone: "13900139000",
          province: "P2",
          city: "C2",
          region: "R2",
          detailAddress: `D2-${RUN_TAG}`,
          isDefault: 0,
        },
      });
      await api(`/user/address/${tempAddressId}`, {
        method: "DELETE",
        token: state.user.token,
      });

      await ensureDefaultAddress();
      return `defaultAddressId=${state.defaultAddressId}`;
    }, { required: true });

    await runCase("Pick product + category reads", async () => {
      const categories = await api("/product/category/list");
      assert(Array.isArray(categories), "Product category list not array", categories);

      const allCategories = await api("/category/list/all");
      assert(Array.isArray(allCategories), "Admin category list not array", allCategories);

      const product = await pickProduct();
      const detail = await api(`/product/detail/${product.id}`);
      assert(Number(detail?.id) === Number(product.id), "Product detail id mismatch", detail);

      if (product.categoryId) {
        const filtered = await api("/product/list", {
          query: { pageNum: 1, pageSize: 20, categoryIds: String(product.categoryId), recentDays: 3650 },
        });
        assert(getRecords(filtered).length >= 1, "Filtered product list is empty");
      }
      return `productId=${product.id}`;
    }, { required: true });
  } catch (e) {
    setupFailed = true;
  }

  if (!setupFailed) {
    await runCase("Cart full operations", async () => {
      await api("/cart/clear", { method: "DELETE", token: state.user.token });
      await api("/cart/add", {
        method: "POST",
        token: state.user.token,
        body: { productId: state.product.id, quantity: 2 },
      });

      const list = await api("/cart/list", { token: state.user.token });
      const item = (Array.isArray(list) ? list : []).find((c) => Number(c?.productId) === Number(state.product.id));
      assert(item?.id, "Cart item missing after add", list);

      await api("/cart/update", {
        method: "PUT",
        token: state.user.token,
        query: { cartItemId: item.id, quantity: 1 },
      });
      await api("/cart/select", {
        method: "PUT",
        token: state.user.token,
        query: { cartItemId: item.id, selected: false },
      });
      await api("/cart/select", {
        method: "PUT",
        token: state.user.token,
        query: { cartItemId: item.id, selected: true },
      });
      await api("/cart/selectAll", {
        method: "PUT",
        token: state.user.token,
        query: { selected: true },
      });

      const count = await api("/cart/count", { token: state.user.token });
      assert(Number(count) >= 1, "Cart count invalid", count);

      await api(`/cart/delete/${item.id}`, {
        method: "DELETE",
        token: state.user.token,
      });
      await api("/cart/clear", { method: "DELETE", token: state.user.token });
      return "cart add/update/select/delete verified";
    });

    await runCase("Main order linkage chain", async () => {
      const order = await createOrderFromCart({ remark: `main:${RUN_TAG}` });
      state.mainOrder = order;

      const pendingPage = await api("/order/list", {
        token: state.user.token,
        query: { pageNum: 1, pageSize: 20, status: 0 },
      });
      const pendingRecords = getRecords(pendingPage);
      assert(pendingRecords.some((o) => Number(o?.id) === Number(order.id)), "Pending order not found");

      await api(`/order/pay/${order.id}`, {
        method: "POST",
        token: state.user.token,
      });

      const adminPage = await api("/admin/order/list", {
        token: state.admin.token,
        query: { pageNum: 1, pageSize: 20, orderNo: order.orderSn },
      });
      const adminHit = getRecords(adminPage).find((o) => Number(o?.id) === Number(order.id));
      assert(adminHit, "Admin order list cannot find order", adminPage);

      await api(`/admin/order/${order.id}`, { token: state.admin.token });
      await api(`/admin/order/${order.id}/address`, {
        method: "PUT",
        token: state.admin.token,
        body: {
          receiver: "Auto Receiver",
          phone: "13800138000",
          address: `Auto Address ${RUN_TAG}`,
        },
      });
      await api(`/admin/order/${order.id}/ship`, {
        method: "PUT",
        token: state.admin.token,
        body: { company: "AutoExpress", trackingNo: TRACKING_NO },
      });

      await waitFor(async () => {
        const detail = await api(`/order/detail/${order.id}`, { token: state.user.token });
        return Number(detail?.status) === 2 ? detail : null;
      }, { label: "order shipped" });

      await api(`/order/confirm/${order.id}`, {
        method: "POST",
        token: state.user.token,
      });

      await waitFor(async () => {
        const detail = await api(`/order/detail/${order.id}`, { token: state.user.token });
        return Number(detail?.status) === 3 ? detail : null;
      }, { label: "order completed" });

      await api(`/order/review/${order.id}`, {
        method: "POST",
        token: state.user.token,
        body: { score: 5, content: `review ${RUN_TAG}` },
      });
      const reviewed = await api(`/order/detail/${order.id}`, { token: state.user.token });
      assert(Number(reviewed?.reviewStatus) === 1, "Order reviewStatus not 1", reviewed);
      return `orderId=${order.id}`;
    });

    await runCase("User note publish + admin audit operations", async () => {
      const noteId = await api("/note/publish", {
        method: "POST",
        token: state.user.token,
        body: {
          title: `Auto note ${RUN_TAG}`,
          content: `Auto note content ${RUN_TAG}`,
          category: "review",
          tags: ["auto", "regression"],
          coverImg: state.product?.coverImg || "",
          images: state.product?.coverImg ? [state.product.coverImg] : [],
          type: "image",
          videoUrl: "",
          productIds: [state.product.id],
        },
      });
      state.noteId = noteId;

      const myNotes = await api("/note/my", {
        token: state.user.token,
        query: { pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(myNotes).some((n) => Number(n?.id) === Number(noteId)), "Published note missing in my notes");

      const pending = await api(`/admin/note/${noteId}`, { token: state.admin.token });
      assert(Number(pending?.status) === 0, "Published note should be pending status=0", pending);

      await api(`/admin/note/${noteId}/approve`, {
        method: "PUT",
        token: state.admin.token,
      });
      await waitFor(async () => {
        const d = await api(`/note/detail/${noteId}`, { token: state.user.token });
        return Number(d?.status) === 1 ? d : null;
      }, { label: "note approved" });

      await api(`/admin/note/${noteId}/sticky`, { method: "PUT", token: state.admin.token });
      await api(`/admin/note/${noteId}/sticky`, { method: "PUT", token: state.admin.token });
      await api(`/admin/note/${noteId}/recommend`, { method: "PUT", token: state.admin.token });
      await api(`/admin/note/${noteId}/recommend`, { method: "PUT", token: state.admin.token });
      await api(`/admin/note/${noteId}/shield`, {
        method: "PUT",
        token: state.admin.token,
        query: { reason: "auto down" },
      });
      await api(`/admin/note/${noteId}/shield`, {
        method: "PUT",
        token: state.admin.token,
      });

      const adminList = await api("/admin/note/list", {
        token: state.admin.token,
        query: { pageNum: 1, pageSize: 20, keyword: RUN_TAG },
      });
      assert(getRecords(adminList).some((n) => Number(n?.id) === Number(noteId)), "Admin note list missing new note");

      await api("/admin/note/stats", { token: state.admin.token });
      return `noteId=${noteId}`;
    });

    await runCase("Peer note + interaction/comment/follow/complaint", async () => {
      const peerNoteId = await createPeerApprovedNote();

      const noteList = await api("/note/list", {
        token: state.user.token,
        query: { pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(noteList).some((n) => Number(n?.id) === Number(peerNoteId)), "Peer note missing in note list");
      await api(`/note/detail/${peerNoteId}`, { token: state.user.token });

      const liked = await api(`/note/like/${peerNoteId}`, {
        method: "POST",
        token: state.user.token,
      });
      assert(liked === true, "First like toggle should be true", liked);
      const myLiked = await api("/note/my/like", {
        token: state.user.token,
        query: { pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(myLiked).some((n) => Number(n?.id) === Number(peerNoteId)), "My liked list missing note");
      const unliked = await api(`/note/like/${peerNoteId}`, {
        method: "POST",
        token: state.user.token,
      });
      assert(unliked === false, "Second like toggle should be false", unliked);

      const collected = await api(`/note/collect/${peerNoteId}`, {
        method: "POST",
        token: state.user.token,
      });
      assert(collected === true, "First collect toggle should be true", collected);
      const myCollect = await api("/note/my/collect", {
        token: state.user.token,
        query: { pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(myCollect).some((n) => Number(n?.id) === Number(peerNoteId)), "My collect list missing note");
      const uncollected = await api(`/note/collect/${peerNoteId}`, {
        method: "POST",
        token: state.user.token,
      });
      assert(uncollected === false, "Second collect toggle should be false", uncollected);

      await api(`/note/recommend/${peerNoteId}`, { method: "POST", token: state.user.token });
      await api(`/note/recommend/${peerNoteId}`, { method: "POST", token: state.user.token });

      const commentId = await api("/comment/add", {
        method: "POST",
        token: state.user.token,
        body: { noteId: peerNoteId, content: `auto comment ${RUN_TAG}` },
      });
      const commentPage = await api("/comment/list", {
        query: { noteId: peerNoteId, pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(commentPage).some((c) => Number(c?.id) === Number(commentId)), "Comment not found in list");
      await api(`/comment/${commentId}`, {
        method: "DELETE",
        token: state.user.token,
      });

      const commentId2 = await api("/comment/add", {
        method: "POST",
        token: state.user.token,
        body: { noteId: peerNoteId, content: `auto admin delete ${RUN_TAG}` },
      });
      const adminCommentPage = await api("/admin/comment/list", {
        token: state.admin.token,
        query: { noteId: peerNoteId, pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(adminCommentPage).some((c) => Number(c?.id) === Number(commentId2)), "Admin comment list missing comment");
      await api(`/admin/comment/${commentId2}`, {
        method: "DELETE",
        token: state.admin.token,
      });

      const beforeFollow = await api(`/follow/status/${state.peer.userId}`, { token: state.user.token });
      const toggled = await api(`/follow/${state.peer.userId}`, {
        method: "POST",
        token: state.user.token,
      });
      const afterFollow = await api(`/follow/status/${state.peer.userId}`, { token: state.user.token });
      assert(afterFollow === toggled, "Follow status mismatch after toggle", { beforeFollow, toggled, afterFollow });
      await api(`/follow/count/${state.peer.userId}`);
      await api(`/follow/followers/${state.peer.userId}`, { query: { pageNum: 1, pageSize: 10 } });
      await api(`/follow/following/${state.user.userId}`, { query: { pageNum: 1, pageSize: 10 } });
      if (afterFollow !== beforeFollow) {
        await api(`/follow/${state.peer.userId}`, {
          method: "POST",
          token: state.user.token,
        });
      }

      const complaintId = await api("/complaint", {
        method: "POST",
        token: state.user.token,
        body: {
          noteId: peerNoteId,
          reason: "inappropriate",
          content: `auto complaint ${RUN_TAG}`,
        },
      });
      state.complaintId = complaintId;

      const latest = await api("/complaint/my/latest", {
        token: state.user.token,
        query: { noteId: peerNoteId },
      });
      assert(Number(latest?.id) === Number(complaintId), "Latest complaint mismatch", latest);

      await api(`/complaint/my/${complaintId}`, { token: state.user.token });
      const complaintList = await api("/admin/complaint/list", {
        token: state.admin.token,
        query: { pageNum: 1, pageSize: 30 },
      });
      assert(getRecords(complaintList).some((c) => Number(c?.id) === Number(complaintId)), "Admin complaint list missing complaint");

      await api(`/admin/complaint/${complaintId}/status`, {
        method: "PUT",
        token: state.admin.token,
        query: { status: 1, remark: "handled by auto test" },
      });
      await api(`/complaint/${complaintId}/feedback`, {
        method: "PUT",
        token: state.user.token,
        body: { feedbackStatus: 1, content: "ok" },
      });
      return `peerNoteId=${peerNoteId}, complaintId=${complaintId}`;
    });

    await runCase("Notification read flow", async () => {
      const unreadBefore = await api("/notification/unread-count", { token: state.user.token });
      const unreadPage = await api("/notification/list", {
        token: state.user.token,
        query: { pageNum: 1, pageSize: 20, unreadOnly: 1 },
      });
      const unreadRecords = getRecords(unreadPage);
      if (unreadRecords[0]?.id) {
        await api(`/notification/${unreadRecords[0].id}/read`, {
          method: "PUT",
          token: state.user.token,
        });
      }
      await api("/notification/read-all", {
        method: "PUT",
        token: state.user.token,
      });
      const unreadAfter = await api("/notification/unread-count", { token: state.user.token });
      assert(Number(unreadAfter) === 0, "Unread count should be 0 after read-all", { unreadBefore, unreadAfter });
      return `unreadBefore=${unreadBefore}, unreadAfter=${unreadAfter}`;
    });

    await runCase("After-sale workflow", async () => {
      const order = await createOrderFromCart({ remark: `after_sale:${RUN_TAG}` });
      state.afterSaleOrder = order;
      await api(`/order/pay/${order.id}`, {
        method: "POST",
        token: state.user.token,
      });

      const afterSaleId = await api("/after-sale/apply", {
        method: "POST",
        token: state.user.token,
        body: {
          orderId: order.id,
          orderItemId: order.itemId,
          type: 0,
          reason: "auto after-sale",
          description: `after-sale ${RUN_TAG}`,
          evidenceImages: [],
        },
      });

      const myList = await api("/after-sale/my/list", {
        token: state.user.token,
        query: { pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(myList).some((a) => Number(a?.id) === Number(afterSaleId)), "After-sale missing in my list");

      const adminList = await api("/admin/after-sale/list", {
        token: state.admin.token,
        query: { pageNum: 1, pageSize: 20 },
      });
      assert(getRecords(adminList).some((a) => Number(a?.id) === Number(afterSaleId)), "After-sale missing in admin list");

      await api(`/admin/after-sale/${afterSaleId}/status`, {
        method: "PUT",
        token: state.admin.token,
        query: { status: 1, remark: "processing" },
      });
      await api(`/after-sale/complete/${afterSaleId}`, {
        method: "POST",
        token: state.user.token,
      });

      const verified = await waitFor(async () => {
        const page = await api("/after-sale/my/list", {
          token: state.user.token,
          query: { pageNum: 1, pageSize: 20 },
        });
        const hit = getRecords(page).find((r) => Number(r?.id) === Number(afterSaleId));
        return Number(hit?.status) === 2 ? hit : null;
      }, { label: "after-sale completed" });

      assert(verified, "After-sale status not completed");
      return `afterSaleId=${afterSaleId}`;
    });

    await runCase("Refund workflow", async () => {
      const order = await createOrderFromCart({ remark: `refund:${RUN_TAG}` });
      state.refundOrder = order;
      await api(`/order/pay/${order.id}`, {
        method: "POST",
        token: state.user.token,
      });

      await api(`/order/cancel/${order.id}`, {
        method: "POST",
        token: state.user.token,
      });

      const refunding = await waitFor(async () => {
        const detail = await api(`/order/detail/${order.id}`, { token: state.user.token });
        return Number(detail?.status) === 5 ? detail : null;
      }, { label: "order refunding status=5" });
      assert(refunding, "Order not in refunding status");

      await api(`/admin/order/${order.id}/refund`, {
        method: "PUT",
        token: state.admin.token,
        body: { success: true, reason: "auto approve", remark: "approved by auto test" },
      });

      const closed = await waitFor(async () => {
        const detail = await api(`/order/detail/${order.id}`, { token: state.user.token });
        return Number(detail?.status) === 4 ? detail : null;
      }, { label: "order closed after refund" });
      assert(closed, "Order not closed after refund handling");
      return `refundOrderId=${order.id}`;
    });

    await runCase("Admin category + banner CRUD", async () => {
      const categoryResp = await api("/category/add", {
        method: "POST",
        body: {
          name: `AutoCategory-${RUN_TAG}`,
          icon: "",
          sort: 9999,
          status: 1,
        },
      });
      assert(categoryResp === true, "Category create failed", categoryResp);

      const all = await api("/category/list/all");
      const createdCategory = (Array.isArray(all) ? all : []).find((c) => String(c?.name || "").includes(`AutoCategory-${RUN_TAG}`));
      assert(createdCategory?.id, "Created category not found in list", all.slice(-5));
      const createdCategoryId = createdCategory.id;
      cleanupTasks.push(async () => {
        await api(`/category/delete/${createdCategoryId}`, { method: "DELETE" });
      });

      await api("/category/update", {
        method: "POST",
        body: {
          id: createdCategoryId,
          name: `AutoCategory-${RUN_TAG}-U`,
          icon: "",
          sort: 9998,
          status: 1,
        },
      });

      const bannerId = await api("/admin/banner", {
        method: "POST",
        token: state.admin.token,
        body: {
          title: `AutoBanner-${RUN_TAG}`,
          position: "SHOP_TOP",
          slot: "main",
          imageUrl: state.product?.coverImg || "/images/common/auto.png",
          keyword: "auto",
          linkUrl: "/shop?auto=1",
          linkType: "internal",
          sort: 9999,
          status: 1,
        },
      });
      assert(bannerId, "Banner create failed");
      cleanupTasks.push(async () => {
        await api(`/admin/banner/${bannerId}`, {
          method: "DELETE",
          token: state.admin.token,
        });
      });

      await api(`/admin/banner/${bannerId}`, {
        method: "PUT",
        token: state.admin.token,
        body: {
          title: `AutoBanner-${RUN_TAG}-U`,
          position: "SHOP_TOP",
          slot: "main",
          imageUrl: state.product?.coverImg || "/images/common/auto.png",
          keyword: "auto2",
          linkUrl: "/shop?auto=2",
          linkType: "internal",
          sort: 9998,
          status: 1,
        },
      });
      await api(`/admin/banner/${bannerId}/status`, {
        method: "PUT",
        token: state.admin.token,
        query: { status: 1 },
      });
      await api("/banner/position/SHOP_TOP");
      return `categoryId=${createdCategoryId}, bannerId=${bannerId}`;
    });

    await runCase("Admin user CRUD", async () => {
      const tempUsername = `usr_${RUN_TAG}`.slice(0, 20);
      const userId = await api("/admin/user", {
        method: "POST",
        token: state.admin.token,
        body: {
          username: tempUsername,
          password: "Abc12345",
          nickname: "Temp User",
          role: "user",
          status: 1,
          phone: "13700137000",
        },
      });
      assert(userId, "Admin create user failed", userId);
      cleanupTasks.push(async () => {
        await api(`/admin/user/${userId}`, {
          method: "DELETE",
          token: state.admin.token,
        });
      });

      await api(`/admin/user/${userId}`, { token: state.admin.token });
      await api(`/admin/user/${userId}`, {
        method: "PUT",
        token: state.admin.token,
        body: { nickname: "Temp User U", role: "user", status: 1 },
      });
      await api(`/admin/user/${userId}/ban`, {
        method: "PUT",
        token: state.admin.token,
        query: { reason: "auto test" },
      });
      await api(`/admin/user/${userId}/unban`, {
        method: "PUT",
        token: state.admin.token,
      });
      const newPassword = await api(`/admin/user/${userId}/reset-password`, {
        method: "POST",
        token: state.admin.token,
      });
      assert(typeof newPassword === "string" && newPassword.length >= 6, "Reset password result invalid", newPassword);
      await api(`/admin/user/${userId}/force-logout`, {
        method: "POST",
        token: state.admin.token,
      });
      await api(`/admin/user/${userId}/harmonize-avatar`, {
        method: "POST",
        token: state.admin.token,
      });

      const list = await api("/admin/user/list", {
        token: state.admin.token,
        query: { pageNum: 1, pageSize: 20, keyword: tempUsername },
      });
      assert(getRecords(list).some((u) => Number(u?.id) === Number(userId)), "Admin user list missing temp user");
      return `tempUserId=${userId}`;
    });

    await runCase("Admin product CRUD + batch", async () => {
      const catList = await api("/product/category/list");
      const category = (Array.isArray(catList) ? catList : [])[0];
      assert(category?.id, "No available category for product create", catList);

      const payload = {
        categoryId: category.id,
        name: `Auto Product ${RUN_TAG}`.slice(0, 60),
        subTitle: "auto",
        price: 19.9,
        stock: 20,
        unit: "piece",
        coverImg: state.product?.coverImg || "/images/common/auto.png",
        coverImgs: JSON.stringify([state.product?.coverImg || "/images/common/auto.png"]),
        detailImgs: JSON.stringify([state.product?.coverImg || "/images/common/auto.png"]),
        description: "auto generated product",
        status: 0,
        warningStock: 2,
        sortWeight: 0,
        petType: "general",
      };
      const productId = await api("/admin/product", {
        method: "POST",
        token: state.admin.token,
        body: payload,
      });
      assert(productId, "Admin create product failed", productId);
      cleanupTasks.push(async () => {
        await api(`/admin/product/${productId}`, {
          method: "DELETE",
          token: state.admin.token,
        });
      });

      await api(`/admin/product/${productId}`, { token: state.admin.token });
      await api(`/admin/product/${productId}`, {
        method: "PUT",
        token: state.admin.token,
        body: {
          ...payload,
          name: `${payload.name} U`,
          price: 29.9,
          stock: 18,
        },
      });
      await api(`/admin/product/${productId}/status`, {
        method: "PUT",
        token: state.admin.token,
        query: { status: 1 },
      });
      await api("/admin/product/batch", {
        method: "POST",
        token: state.admin.token,
        query: { action: "offline" },
        body: [productId],
      });

      const list = await api("/admin/product/list", {
        token: state.admin.token,
        query: { pageNum: 1, pageSize: 20, keyword: "Auto Product" },
      });
      assert(getRecords(list).some((p) => Number(p?.id) === Number(productId)), "Admin product list missing temp product");
      return `tempProductId=${productId}`;
    });

    await runCase("Dashboard + order export", async () => {
      await api("/admin/dashboard/stats", { token: state.admin.token, query: { range: "today" } });
      await api("/admin/dashboard/stats", { token: state.admin.token, query: { range: "week" } });
      await api("/admin/dashboard/stats", { token: state.admin.token, query: { range: "month" } });
      await api("/admin/dashboard/trend", { token: state.admin.token, query: { range: "week" } });
      await api("/admin/dashboard/trend", { token: state.admin.token, query: { range: "month" } });
      await api("/admin/dashboard/category-sales", { token: state.admin.token });
      await api("/admin/dashboard/top-products", { token: state.admin.token });
      await api("/admin/dashboard/todos", { token: state.admin.token });
      const exportPath = await api("/admin/order/export", { token: state.admin.token });
      assert(typeof exportPath === "string", "Export path invalid", exportPath);
      return `export=${exportPath}`;
    });

    await runCase("Upload endpoints", async () => {
      const blob = makeTinyPngBlob();

      const form1 = new FormData();
      form1.append("file", blob, "tiny.png");
      form1.append("biz", "noteImage");
      const single = await api("/common/upload/image", {
        method: "POST",
        formData: form1,
      });
      assert(typeof single === "string" && single.includes("/images/"), "Single upload url invalid", single);

      const form2 = new FormData();
      form2.append("files", blob, "tiny1.png");
      form2.append("files", blob, "tiny2.png");
      form2.append("biz", "common");
      const batch = await api("/common/upload/batch", {
        method: "POST",
        formData: form2,
      });
      assert(Array.isArray(batch) && batch.length >= 2, "Batch upload result invalid", batch);
      return `single=${single}`;
    });
  } else {
    results.push({
      name: "Optional modules",
      status: "SKIP",
      ms: 0,
      detail: "Required setup failed, optional modules skipped",
    });
  }

  await safeCleanup();

  try {
    if (state.user.token) {
      await api("/auth/logout", { method: "POST", token: state.user.token });
    }
    if (state.peer.token) {
      await api("/auth/logout", { method: "POST", token: state.peer.token });
    }
    if (state.admin.token) {
      await api("/admin/auth/logout", { method: "POST", token: state.admin.token });
    }
  } catch (e) {
    console.error(`[WARN] logout failed: ${e?.message || e}`);
  }

  const passed = results.filter((r) => r.status === "PASS").length;
  const failed = results.filter((r) => r.status === "FAIL").length;
  const skipped = results.filter((r) => r.status === "SKIP").length;

  console.log("\n=== REGRESSION SUMMARY ===");
  for (const r of results) {
    console.log(`- [${r.status}] ${r.name} (${r.ms}ms)${r.detail ? ` :: ${r.detail}` : ""}`);
  }
  console.log(`\n[STATS] pass=${passed}, fail=${failed}, skip=${skipped}`);

  const artifacts = {
    runTag: RUN_TAG,
    baseUrl: BASE_URL,
    user: state.user.username,
    admin: state.admin.username,
    peer: state.peer.username,
    productId: state.product?.id || null,
    mainOrderId: state.mainOrder?.id || null,
    noteId: state.noteId || null,
    peerNoteId: state.peer.noteId || null,
    complaintId: state.complaintId || null,
    afterSaleOrderId: state.afterSaleOrder?.id || null,
    refundOrderId: state.refundOrder?.id || null,
  };
  console.log("[ARTIFACTS]", JSON.stringify(artifacts, null, 2));

  if (failed > 0) {
    process.exit(1);
  }
}

main().catch((e) => {
  console.error("[FATAL]", e?.message || e);
  if (e?.detail !== undefined) {
    console.error("[DETAIL]", shortJson(e.detail));
  }
  process.exit(1);
});
