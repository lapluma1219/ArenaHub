const SLOTS = ["A", "B"];
const STORAGE_KEY = "arenaHubIndependentSessions";
const POLL_INTERVAL_MS = 1500;

const state = {
  A: createPlayerState("A"),
  B: createPlayerState("B"),
};

function createPlayerState(slot) {
  return {
    slot,
    token: "",
    player: null,
    match: { state: "IDLE", roomId: null, opponent: null },
    room: null,
    result: null,
    busy: false,
    polling: false,
    leaderboardVisible: false,
  };
}

function $(id) {
  return document.getElementById(id);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function loadSessions() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
    SLOTS.forEach((slot) => {
      state[slot].token = saved[slot]?.token || "";
      state[slot].player = saved[slot]?.player || null;
    });
  } catch {
    localStorage.removeItem(STORAGE_KEY);
  }
}

function saveSessions() {
  const saved = {};
  SLOTS.forEach((slot) => {
    saved[slot] = {
      token: state[slot].token,
      player: state[slot].player,
    };
  });
  localStorage.setItem(STORAGE_KEY, JSON.stringify(saved));
}

async function api(path, options = {}, slot = null) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (slot && state[slot].token) {
    headers.Authorization = `Bearer ${state[slot].token}`;
  }

  const response = await fetch(path, { ...options, headers });
  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload?.success) {
    const error = new Error(payload?.message || `请求失败：${response.status}`);
    error.status = response.status;
    throw error;
  }
  return payload.data;
}

function inputValue(slot, field) {
  return $(`${field}-${slot}`).value.trim();
}

async function login(slot) {
  const auth = await api("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({
      username: inputValue(slot, "username"),
      password: inputValue(slot, "password"),
    }),
  });
  await applyAuth(slot, auth);
  setNotice(slot, "登录成功，可以开始匹配。", "success");
}

async function register(slot) {
  const auth = await api("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({
      username: inputValue(slot, "username"),
      password: inputValue(slot, "password"),
      nickname: inputValue(slot, "nickname"),
    }),
  });
  await applyAuth(slot, auth);
  setNotice(slot, "注册并登录成功，可以开始匹配。", "success");
}

async function applyAuth(slot, auth) {
  state[slot].token = auth.token;
  state[slot].player = auth.player;
  state[slot].match = { state: "IDLE", roomId: null, opponent: null };
  state[slot].room = null;
  state[slot].result = null;
  saveSessions();
  renderSlot(slot);
  await syncMatchStatus(slot);
}

async function logout(slot) {
  if (state[slot].match.state === "WAITING") {
    await api("/api/matchmaking/leave", { method: "POST" }, slot).catch(() => {});
  }
  state[slot] = createPlayerState(slot);
  saveSessions();
  renderSlot(slot);
}

async function refreshMe(slot) {
  state[slot].player = await api("/api/players/me", {}, slot);
  saveSessions();
  renderProfile(slot);
}

async function toggleMatch(slot) {
  if (state[slot].match.state === "WAITING") {
    const match = await api("/api/matchmaking/leave", { method: "POST" }, slot);
    applyMatch(slot, match);
    setNotice(slot, "已退出匹配队列。", "");
    return;
  }

  state[slot].room = null;
  state[slot].result = null;
  const match = await api("/api/matchmaking/join", { method: "POST" }, slot);
  applyMatch(slot, match);
  if (match.state === "WAITING") {
    setNotice(slot, "正在等待另一名玩家加入……", "info");
  } else {
    setNotice(slot, `匹配成功，进入房间 #${match.roomId}。`, "success");
    await syncRoom(slot);
  }
}

function applyMatch(slot, match) {
  const previousRoomId = state[slot].match.roomId;
  state[slot].match = match;
  if (!match.roomId || match.roomId !== previousRoomId) {
    state[slot].room = null;
    state[slot].result = null;
  }
  renderMatch(slot);
}

async function syncMatchStatus(slot) {
  if (!state[slot].token) return;
  const match = await api("/api/matchmaking/status", {}, slot);
  applyMatch(slot, match);
  if (match.roomId) {
    await syncRoom(slot);
  }
}

async function syncRoom(slot) {
  const roomId = state[slot].match.roomId || state[slot].room?.id;
  if (!roomId || !state[slot].token) return;

  const room = await api(`/api/rooms/${roomId}`, {}, slot);
  state[slot].room = room;
  if (room.status === "FINISHED") {
    await handleFinishedRoom(slot, room);
  } else {
    renderMatch(slot);
  }
}

async function claimWin(slot) {
  const current = state[slot];
  if (!current.room || current.room.status !== "ACTIVE") {
    throw new Error("当前没有进行中的对局");
  }

  try {
    const record = await api(`/api/rooms/${current.room.id}/finish`, {
      method: "POST",
      body: JSON.stringify({ winnerPlayerId: current.player.id }),
    }, slot);
    current.result = record;
    current.room = { ...current.room, status: "FINISHED", finishedAt: record.finishedAt };
    setNotice(slot, "你率先确认获胜，对局已结算。", "success");
  } catch (error) {
    if (error.status !== 409) throw error;
    setNotice(slot, "对手先完成了结算，正在同步结果。", "info");
    await syncRoom(slot);
  }

  await Promise.all([
    refreshMe(slot),
    loadLeaderboard(slot, true),
  ]);
  renderMatch(slot);
}

async function handleFinishedRoom(slot, room) {
  if (!state[slot].result) {
    const records = await api("/api/matches/me", {}, slot);
    state[slot].result = records.find((record) => record.roomId === room.id) || null;
  }
  await Promise.all([
    refreshMe(slot),
    loadLeaderboard(slot, true),
  ]);
  const won = state[slot].result?.winner.id === state[slot].player.id;
  setNotice(slot, won ? "对局结束：你获胜了。" : "对局结束：对手先确认了胜利。", won ? "success" : "info");
  renderMatch(slot);
}

async function loadLeaderboard(slot, reveal = true) {
  const players = await api("/api/leaderboard?limit=10", {}, slot);
  state[slot].leaderboardVisible = reveal;
  renderLeaderboard(slot, players);
}

async function pollSlot(slot) {
  const current = state[slot];
  if (!current.token || current.polling || current.busy) return;
  if (current.match.state !== "WAITING" && current.room?.status !== "ACTIVE") return;

  current.polling = true;
  try {
    if (current.match.state === "WAITING") {
      const match = await api("/api/matchmaking/status", {}, slot);
      applyMatch(slot, match);
      if (match.state === "MATCHED") {
        setNotice(slot, `匹配成功，进入房间 #${match.roomId}。`, "success");
        await syncRoom(slot);
      }
    } else if (current.room?.status === "ACTIVE") {
      await syncRoom(slot);
    }
  } catch (error) {
    if (error.status === 401) {
      await logout(slot);
      setNotice(slot, "登录已失效，请重新登录。", "error");
    }
  } finally {
    current.polling = false;
  }
}

function renderSlot(slot) {
  const current = state[slot];
  const loggedIn = Boolean(current.token && current.player);
  $(`authView-${slot}`).classList.toggle("hidden", loggedIn);
  $(`gameView-${slot}`).classList.toggle("hidden", !loggedIn);
  setBadge($(`sessionBadge-${slot}`), loggedIn ? "已登录" : "未登录", loggedIn ? "success" : "neutral");
  if (!loggedIn) return;
  renderProfile(slot);
  renderMatch(slot);
}

function renderProfile(slot) {
  const player = state[slot].player;
  if (!player) return;
  $(`profile-${slot}`).innerHTML = [
    metric("昵称", player.nickname),
    metric("积分", player.rating),
    metric("胜场", player.wins),
    metric("负场", player.losses),
  ].join("");
}

function renderMatch(slot) {
  const current = state[slot];
  const badge = $(`matchBadge-${slot}`);
  const content = $(`matchContent-${slot}`);
  const matchButton = $(`matchBtn-${slot}`);
  const winButton = $(`winBtn-${slot}`);
  const room = current.room;

  content.className = "";
  matchButton.textContent = "开始匹配";
  winButton.classList.add("hidden");

  if (current.match.state === "WAITING") {
    setBadge(badge, "等待中", "warning");
    matchButton.textContent = "取消匹配";
    content.innerHTML = '<div class="waiting"><span class="pulse"></span><strong>正在寻找对手……</strong></div>';
    return;
  }

  if (room?.status === "ACTIVE") {
    const opponent = opponentFor(slot, room);
    setBadge(badge, `对局中 #${room.id}`, "success");
    content.innerHTML = `
      <div class="versus">
        <div><span>我</span><strong>${escapeHtml(current.player.nickname)}</strong><small>${current.player.rating} 分</small></div>
        <b>VS</b>
        <div><span>对手</span><strong>${escapeHtml(opponent.nickname)}</strong><small>${opponent.rating} 分</small></div>
      </div>
      <p class="match-hint">确认本局胜利后将立即结算积分。</p>
    `;
    winButton.classList.remove("hidden");
    return;
  }

  if (room?.status === "FINISHED" && current.result) {
    const won = current.result.winner.id === current.player.id;
    setBadge(badge, "已结束", won ? "success" : "danger");
    content.innerHTML = `
      <div class="result ${won ? "won" : "lost"}">
        <strong>${won ? "胜利" : "失败"}</strong>
        <p>${escapeHtml(current.result.winner.nickname)} 获胜</p>
        <small>结算积分：${current.result.winnerRatingAfter} / ${current.result.loserRatingAfter}</small>
      </div>
    `;
    return;
  }

  setBadge(badge, "未匹配", "neutral");
  content.className = "empty-state";
  content.textContent = "点击“开始匹配”进入队列。";
}

function renderLeaderboard(slot, players) {
  const card = $(`leaderboardCard-${slot}`);
  card.classList.toggle("hidden", !state[slot].leaderboardVisible);
  const container = $(`leaderboard-${slot}`);
  if (!players.length) {
    container.innerHTML = '<p class="empty-state">暂无排行数据</p>';
    return;
  }
  container.innerHTML = `
    <table class="ranking-table">
      <thead><tr><th>#</th><th>玩家</th><th>积分</th><th>胜/负</th></tr></thead>
      <tbody>
        ${players.map((player, index) => `
          <tr class="${player.id === state[slot].player.id ? "is-me" : ""}">
            <td>${index + 1}</td>
            <td>${escapeHtml(player.nickname)}</td>
            <td>${player.rating}</td>
            <td>${player.wins}/${player.losses}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function opponentFor(slot, room) {
  return room.playerOne.id === state[slot].player.id ? room.playerTwo : room.playerOne;
}

function metric(label, value) {
  return `<div class="stat"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`;
}

function setBadge(element, text, type) {
  element.textContent = text;
  element.className = `badge ${type}`;
}

function setNotice(slot, message, type = "") {
  const notice = $(`notice-${slot}`);
  if (!notice) return;
  notice.textContent = message;
  notice.className = `notice ${type}`.trim();
}

function setSlotBusy(slot, busy) {
  state[slot].busy = busy;
  document.querySelectorAll(`#pane-${slot} button`).forEach((button) => {
    button.disabled = busy;
  });
}

async function runSlotAction(slot, action) {
  if (state[slot].busy) return;
  setSlotBusy(slot, true);
  try {
    await action();
  } catch (error) {
    setNotice(slot, error.message || "操作失败", "error");
  } finally {
    setSlotBusy(slot, false);
  }
}

async function restoreSession(slot) {
  if (!state[slot].token) {
    renderSlot(slot);
    return;
  }
  try {
    await refreshMe(slot);
    await syncMatchStatus(slot);
  } catch {
    state[slot] = createPlayerState(slot);
    saveSessions();
    renderSlot(slot);
  }
}

async function checkHealth() {
  try {
    const response = await fetch("/api/ping");
    if (!response.ok) throw new Error();
    setBadge($("healthBadge"), "服务正常", "success");
  } catch {
    setBadge($("healthBadge"), "服务异常", "danger");
  }
}

function bindEvents() {
  document.querySelectorAll("[data-action]").forEach((button) => {
    button.addEventListener("click", () => {
      const slot = button.dataset.slot;
      const actions = {
        login: () => login(slot),
        register: () => register(slot),
        logout: () => logout(slot),
        match: () => toggleMatch(slot),
        leaderboard: () => loadLeaderboard(slot),
        win: () => claimWin(slot),
      };
      runSlotAction(slot, actions[button.dataset.action]);
    });
  });

  $("resetDemoBtn").addEventListener("click", async () => {
    await Promise.all(SLOTS.map((slot) => state[slot].match.state === "WAITING" ? logout(slot) : Promise.resolve()));
    localStorage.removeItem(STORAGE_KEY);
    location.reload();
  });
}

async function init() {
  loadSessions();
  bindEvents();
  SLOTS.forEach(renderSlot);
  await checkHealth();
  await Promise.all(SLOTS.map(restoreSession));
  setInterval(() => SLOTS.forEach(pollSlot), POLL_INTERVAL_MS);
}

init();
