'use strict';

// Change YOUR_BACKEND_NAME when deploying to Render!
const API = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' 
    ? 'http://localhost:8080/api/v1/triage' 
    : 'https://triage-engine-ub5p.onrender.com/api/v1/triage';

/* ===========================
   STATE
   =========================== */
let currentPage = 'dashboard';
let allCasesData = [];
let currentFilter = 'ALL';
let currentSort = 'score-desc';
let symptoms = [];
let sidebarOpen = window.innerWidth > 768;

/* ===========================
   QUICK SYMPTOM PRESETS
   =========================== */
const QUICK_SYMPTOMS = [
  'chest pain', 'shortness of breath', 'fever', 'dizziness',
  'headache', 'nausea', 'vomiting', 'palpitations', 'weakness',
  'numbness', 'confusion', 'back pain', 'abdominal pain', 'cough'
];

/* ===========================
   INIT
   =========================== */
document.addEventListener('DOMContentLoaded', () => {
  initClock();
  initSession();
  initNav();
  initForm();
  initModal();
  initSearch();
  populateQuickSymptoms();
  loadDashboard();
  checkApi();

  document.getElementById('menu-toggle').addEventListener('click', toggleSidebar);
  document.getElementById('btn-refresh').addEventListener('click', refreshCurrentPage);
  document.getElementById('result-close').addEventListener('click', closeResultOverlay);
  document.getElementById('modal-close').addEventListener('click', closeModal);
  document.getElementById('modal-overlay').addEventListener('click', e => {
    if (e.target === document.getElementById('modal-overlay')) closeModal();
  });
});

/* ===========================
   CLOCK
   =========================== */
function initClock() {
  function tick() {
    const now = new Date();
    const t = now.toLocaleTimeString('en-IN', { hour12: false });
    const d = now.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    const el = document.getElementById('sidebar-time');
    if (el) el.textContent = t;
    const tdr = document.getElementById('topbar-date');
    if (tdr) tdr.textContent = d + ' · ' + t;
  }
  tick();
  setInterval(tick, 1000);
}

/* ===========================
   SESSION / AUTH
   =========================== */
function initSession() {
  const raw = localStorage.getItem('triage_user');
  if (!raw) { window.location.href = 'login.html'; return; }
  try {
    const user = JSON.parse(raw);
    const initials = user.name ? user.name.split(' ').map(w => w[0]).join('').slice(0,2).toUpperCase() : 'DR';
    // Sidebar
    const av = document.getElementById('user-avatar');
    const nm = document.getElementById('user-name');
    const rl = document.getElementById('user-role');
    if (av) av.textContent = initials;
    if (nm) nm.textContent = user.name || 'Dr. Rivera';
    if (rl) rl.textContent = user.role || 'Emergency Physician';
    // Topbar
    const tav = document.getElementById('topbar-avatar');
    const tun = document.getElementById('topbar-username');
    const trl = document.getElementById('topbar-role');
    if (tav) tav.textContent = initials;
    if (tun) tun.textContent = user.name || 'Dr. Rivera';
    if (trl) trl.textContent = user.role || 'Emergency Physician';
  } catch(e) {}

  function doLogout() {
    localStorage.removeItem('triage_user');
    window.location.href = 'login.html';
  }
  document.getElementById('btn-logout')?.addEventListener('click', doLogout);
  document.getElementById('btn-logout-top')?.addEventListener('click', doLogout);
}


/* ===========================
   SIDEBAR
   =========================== */
function toggleSidebar() {
  const sb = document.getElementById('sidebar');
  const mc = document.getElementById('main-content');
  if (window.innerWidth <= 768) {
    sb.classList.toggle('open');
  } else {
    sidebarOpen = !sidebarOpen;
    if (sidebarOpen) {
      sb.classList.remove('collapsed');
      mc.classList.remove('full-width');
    } else {
      sb.classList.add('collapsed');
      mc.classList.add('full-width');
    }
  }
}

/* ===========================
   NAVIGATION
   =========================== */
function initNav() {
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', e => {
      e.preventDefault();
      navigateTo(item.dataset.page);
      if (window.innerWidth <= 768) {
        document.getElementById('sidebar').classList.remove('open');
      }
    });
  });
}

function navigateTo(page) {
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));

  const navEl = document.getElementById('nav-' + page);
  if (navEl) navEl.classList.add('active');
  const pageEl = document.getElementById('page-' + page);
  if (pageEl) pageEl.classList.add('active');

  const bc = document.getElementById('breadcrumb-current');
  if (bc) {
    bc.textContent = { dashboard:'Dashboard', submit:'Submit Case', cases:'All Cases', urgent:'Urgent Cases' }[page] || page;
  }

  currentPage = page;

  if (page === 'dashboard') loadDashboard();
  else if (page === 'cases') loadCases();
  else if (page === 'urgent') loadUrgent();
}

function filterAndNavigate(priority) {
  navigateTo('cases');
  setTimeout(() => setFilter(priority), 100);
}

function refreshCurrentPage() {
  const btn = document.getElementById('btn-refresh');
  btn.style.animation = 'none';
  void btn.offsetWidth;
  if (currentPage === 'dashboard') loadDashboard();
  else if (currentPage === 'cases') loadCases();
  else if (currentPage === 'urgent') loadUrgent();
}

/* ===========================
   API HEALTH CHECK
   =========================== */
async function checkApi() {
  try {
    const res = await fetch(API + '/dashboard', { signal: AbortSignal.timeout(3000) });
    const ok = res.ok;
    const dot = document.querySelector('.status-dot');
    const txt = document.querySelector('.status-text');
    if (ok) {
      dot.className = 'status-dot pulse-green';
      txt.textContent = 'API Connected';
    } else {
      dot.className = 'status-dot error';
      txt.textContent = 'API Error';
    }
  } catch {
    const dot = document.querySelector('.status-dot');
    const txt = document.querySelector('.status-text');
    dot.className = 'status-dot error';
    txt.textContent = 'API Offline';
  }
}

/* ===========================
   DASHBOARD
   =========================== */
async function loadDashboard() {
  try {
    const [dashboard, cases] = await Promise.all([
      fetch(API + '/dashboard').then(r => r.json()),
      fetch(API + '/cases').then(r => r.json())
    ]);

    const total = dashboard.totalCases ?? 0;
    const critical = dashboard.critical ?? 0;
    const high = dashboard.high ?? 0;
    const medium = dashboard.medium ?? 0;
    const low = dashboard.low ?? 0;

    animateCount('stat-total-val', total);
    animateCount('stat-critical-val', critical);
    animateCount('stat-high-val', high);
    animateCount('stat-medium-val', medium);
    animateCount('stat-low-val', low);

    // Update nav badges
    document.getElementById('cases-badge').textContent = total;
    document.getElementById('urgent-badge').textContent = critical + high;

    // Progress bars
    const pct = (n) => total > 0 ? Math.round((n / total) * 100) : 0;
    setTimeout(() => {
      setWidth('fill-critical', pct(critical));
      setWidth('fill-high', pct(high));
      setWidth('fill-medium', pct(medium));
      setWidth('fill-low', pct(low));
      setWidth('dist-critical', pct(critical));
      setWidth('dist-high', pct(high));
      setWidth('dist-medium', pct(medium));
      setWidth('dist-low', pct(low));
    }, 100);

    document.getElementById('dist-critical-count').textContent = critical;
    document.getElementById('dist-high-count').textContent = high;
    document.getElementById('dist-medium-count').textContent = medium;
    document.getElementById('dist-low-count').textContent = low;

    const emptyChart = document.getElementById('empty-chart-msg');
    const distChart = document.getElementById('distribution-chart');
    if (total === 0) {
      emptyChart.style.display = 'flex';
      distChart.style.display = 'none';
    } else {
      emptyChart.style.display = 'none';
      distChart.style.display = 'flex';
    }

    // Recent cases (latest 5)
    const recent = [...(cases || [])].slice(0, 5);
    const recentList = document.getElementById('recent-cases-list');
    const emptyRecent = document.getElementById('empty-recent');
    if (recent.length === 0) {
      recentList.innerHTML = '';
      emptyRecent.style.display = 'flex';
    } else {
      emptyRecent.style.display = 'none';
      recentList.innerHTML = recent.map(c => buildCaseCard(c, true)).join('');
      attachCaseCardListeners(recentList);
    }

  } catch (e) {
    showToast('Failed to load dashboard', 'error');
    console.error(e);
  }
}

function setWidth(id, pct) {
  const el = document.getElementById(id);
  if (el) el.style.width = pct + '%';
}

function animateCount(id, target) {
  const el = document.getElementById(id);
  if (!el) return;
  const start = parseInt(el.textContent) || 0;
  const dur = 600;
  const startTime = performance.now();
  function step(now) {
    const prog = Math.min((now - startTime) / dur, 1);
    el.textContent = Math.round(start + (target - start) * easeOut(prog));
    if (prog < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

function easeOut(t) { return 1 - Math.pow(1 - t, 3); }

/* ===========================
   ALL CASES
   =========================== */
async function loadCases() {
  const list = document.getElementById('cases-list');
  const empty = document.getElementById('empty-cases');
  const loading = document.getElementById('cases-loading');
  list.innerHTML = '';
  empty.style.display = 'none';
  loading.style.display = 'flex';

  try {
    let data;
    const url = currentFilter === 'ALL'
      ? API + '/cases'
      : API + '/cases/priority/' + currentFilter;
    const res = await fetch(url);
    if (!res.ok) throw new Error('HTTP ' + res.status);
    data = await res.json();
    allCasesData = data || [];
    loading.style.display = 'none';
    renderCases();
  } catch (e) {
    loading.style.display = 'none';
    console.error('loadCases error:', e);
    const isCors = e instanceof TypeError;
    showToast(
      isCors ? 'CORS error — restart backend after the SecurityConfig update' : ('Failed to load cases: ' + e.message),
      'error'
    );
    document.getElementById('empty-cases').style.display = 'flex';
  }
}

function renderCases() {
  const list = document.getElementById('cases-list');
  const empty = document.getElementById('empty-cases');
  const search = document.getElementById('cases-search').value.toLowerCase();
  const sort = document.getElementById('cases-sort').value;

  let data = [...allCasesData];

  if (search) {
    data = data.filter(c =>
      c.name.toLowerCase().includes(search) ||
      (c.chiefComplaint || '').toLowerCase().includes(search) ||
      (c.symptoms || []).some(s => s.toLowerCase().includes(search))
    );
  }

  data.sort((a, b) => {
    if (sort === 'score-desc') return b.triageScore - a.triageScore;
    if (sort === 'score-asc') return a.triageScore - b.triageScore;
    if (sort === 'time-desc') return new Date(b.submittedAt) - new Date(a.submittedAt);
    if (sort === 'time-asc') return new Date(a.submittedAt) - new Date(b.submittedAt);
    return 0;
  });

  if (data.length === 0) {
    list.innerHTML = '';
    empty.style.display = 'flex';
  } else {
    empty.style.display = 'none';
    list.innerHTML = data.map(c => buildCaseCard(c, false)).join('');
    attachCaseCardListeners(list);
  }
}

function setFilter(f) {
  currentFilter = f;
  document.querySelectorAll('.filter-tab').forEach(t => {
    t.classList.toggle('active', t.dataset.filter === f);
  });
  loadCases();
}

/* ===========================
   URGENT CASES
   =========================== */
async function loadUrgent() {
  const list = document.getElementById('urgent-list');
  const empty = document.getElementById('empty-urgent');
  const loading = document.getElementById('urgent-loading');
  const banner = document.getElementById('urgent-count-text');
  list.innerHTML = '';
  empty.style.display = 'none';
  loading.style.display = 'flex';

  try {
    const res = await fetch(API + '/cases/urgent');
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const data = await res.json();
    loading.style.display = 'none';
    if (!data || data.length === 0) {
      empty.style.display = 'flex';
      banner.textContent = 'No urgent cases — all patients are stable';
    } else {
      banner.textContent = data.length + ' urgent patient(s) require immediate or urgent attention';
      list.innerHTML = data.map(c => buildCaseCard(c, false)).join('');
      attachCaseCardListeners(list);
    }
  } catch (e) {
    loading.style.display = 'none';
    console.error('loadUrgent error:', e);
    const isCors = e instanceof TypeError;
    showToast(
      isCors ? 'CORS error — restart backend after the SecurityConfig update' : ('Failed to load urgent cases: ' + e.message),
      'error'
    );
    empty.style.display = 'flex';
    banner.textContent = 'Could not load urgent cases — check backend connection';
  }
}

/* ===========================
   CASE CARD BUILDER
   =========================== */
function buildCaseCard(c, compact) {
  const pl = c.priorityLevel || 'LOW';
  const syms = (c.symptoms || []).slice(0, compact ? 3 : 5).map(s =>
    `<span class="symptom-chip">${esc(s)}</span>`).join('');
  const vitals = compact ? '' : `
    <div class="case-vitals">
      <span class="vital-chip">❤ ${c.heartRate} bpm</span>
      <span class="vital-chip">🩸 ${c.systolicBP}/${c.diastolicBP} mmHg</span>
      <span class="vital-chip">🌡 ${c.temperature}°C</span>
      <span class="vital-chip">💨 ${c.respiratoryRate} br/min</span>
      <span class="vital-chip">O₂ ${c.oxygenSaturation}%</span>
    </div>`;
  const complaint = compact
    ? `<div class="case-complaint">${esc(c.chiefComplaint || '')}</div>`
    : `<div class="case-complaint">"${esc(c.chiefComplaint || '')}"</div>`;

  return `
  <div class="case-card p-${pl}" data-id="${c.id}">
    <div class="case-card-top">
      <div>
        <div class="case-patient-name">${esc(c.name)}</div>
        <div class="case-meta">
          <span>${c.age}y</span><span class="case-meta-sep">·</span>
          <span>${c.gender}</span><span class="case-meta-sep">·</span>
          <span>ID #${c.id}</span><span class="case-meta-sep">·</span>
          <span>${timeAgo(c.submittedAt)}</span>
        </div>
      </div>
      <div class="case-right">
        <span class="priority-badge badge-${pl}">${pl}</span>
        <span class="score-pill">Score: ${c.triageScore}/100</span>
      </div>
    </div>
    ${vitals}
    <div class="case-symptoms">${syms}</div>
    ${complaint}
  </div>`;
}

function attachCaseCardListeners(container) {
  container.querySelectorAll('.case-card').forEach(card => {
    card.addEventListener('click', () => openCaseModal(parseInt(card.dataset.id)));
  });
}

/* ===========================
   CASE DETAIL MODAL
   =========================== */
async function openCaseModal(id) {
  try {
    const c = await fetch(API + '/' + id).then(r => r.json());
    const pl = c.priorityLevel || 'LOW';
    const rationale = (c.priorityRationale || '').split(';').filter(Boolean);

    document.getElementById('modal-inner').innerHTML = `
      <div class="modal-header">
        <div class="modal-patient-name" id="modal-patient-name">${esc(c.name)}</div>
        <div class="modal-meta">${c.age} years · ${c.gender} · Patient ID #${c.id} · ${formatDate(c.submittedAt)}</div>
        <div class="modal-priority-row">
          <span class="priority-badge badge-${pl}">${pl}</span>
          <span class="modal-score">Triage Score: <strong>${c.triageScore}/100</strong></span>
        </div>
      </div>
      <div class="modal-section">
        <h5>Vital Signs</h5>
        <div class="modal-vitals">
          <div class="vital-box">
            <div class="vital-box-val">${c.heartRate}</div>
            <div class="vital-box-label">Heart Rate (bpm)</div>
          </div>
          <div class="vital-box">
            <div class="vital-box-val">${c.systolicBP}/${c.diastolicBP}</div>
            <div class="vital-box-label">BP (mmHg)</div>
          </div>
          <div class="vital-box">
            <div class="vital-box-val">${c.temperature}°C</div>
            <div class="vital-box-label">Temperature</div>
          </div>
          <div class="vital-box">
            <div class="vital-box-val">${c.respiratoryRate}</div>
            <div class="vital-box-label">Resp. Rate (br/min)</div>
          </div>
          <div class="vital-box">
            <div class="vital-box-val">${c.oxygenSaturation}%</div>
            <div class="vital-box-label">SpO₂</div>
          </div>
          <div class="vital-box">
            <div class="vital-box-val">${c.triageScore}</div>
            <div class="vital-box-label">Triage Score</div>
          </div>
        </div>
      </div>
      <div class="modal-section">
        <h5>Symptoms</h5>
        <div class="case-symptoms" style="margin-bottom:8px">
          ${(c.symptoms || []).map(s => `<span class="symptom-chip">${esc(s)}</span>`).join('')}
        </div>
      </div>
      <div class="modal-section">
        <h5>Chief Complaint</h5>
        <div class="modal-rationale">"${esc(c.chiefComplaint || '')}"</div>
      </div>
      <div class="modal-section">
        <h5>Priority: ${pl} — ${esc(c.priorityDescription || '')}</h5>
        <div class="modal-rationale">
          ${rationale.map(r => `<div class="modal-rationale-item">${esc(r.trim())}</div>`).join('')}
        </div>
      </div>`;

    document.getElementById('modal-overlay').classList.add('open');
  } catch (e) {
    showToast('Failed to load case details', 'error');
  }
}

function initModal() {
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') { closeModal(); closeResultOverlay(); }
  });
}

function closeModal() {
  document.getElementById('modal-overlay').classList.remove('open');
}

/* ===========================
   SEARCH & SORT
   =========================== */
function initSearch() {
  document.getElementById('cases-search').addEventListener('input', renderCases);
  document.getElementById('cases-sort').addEventListener('change', e => {
    currentSort = e.target.value;
    renderCases();
  });
  document.querySelectorAll('.filter-tab').forEach(tab => {
    tab.addEventListener('click', () => setFilter(tab.dataset.filter));
  });
}

/* ===========================
   FORM
   =========================== */
function initForm() {
  const form = document.getElementById('triage-form');
  form.addEventListener('submit', handleSubmit);

  // Character counter
  const complaint = document.getElementById('f-complaint');
  complaint.addEventListener('input', () => {
    document.getElementById('complaint-chars').textContent = complaint.value.length;
    updatePreview();
  });

  // Symptom add
  document.getElementById('btn-add-symptom').addEventListener('click', addSymptomFromInput);
  document.getElementById('symptom-input').addEventListener('keydown', e => {
    if (e.key === 'Enter' || e.key === '+') { e.preventDefault(); addSymptomFromInput(); }
  });

  // Vitals live preview
  ['f-heartrate', 'f-systolicbp', 'f-diastolicbp', 'f-temperature', 'f-resprate', 'f-spo2'].forEach(id => {
    document.getElementById(id)?.addEventListener('input', () => {
      updateVitalIndicator(id);
      updatePreview();
    });
  });

  form.querySelectorAll('input,select,textarea').forEach(el => {
    el.addEventListener('blur', () => validateField(el));
  });
}

function addSymptomFromInput() {
  const inp = document.getElementById('symptom-input');
  const val = inp.value.trim();
  if (!val) return;
  if (symptoms.includes(val.toLowerCase())) { showToast('Symptom already added', 'info'); inp.value = ''; return; }
  symptoms.push(val.toLowerCase());
  inp.value = '';
  renderSymptomTags();
  updatePreview();
}

function removeSymptom(s) {
  symptoms = symptoms.filter(x => x !== s);
  renderSymptomTags();
  updatePreview();
}

function renderSymptomTags() {
  const container = document.getElementById('symptom-tags');
  container.innerHTML = symptoms.map(s => `
    <span class="symptom-tag">
      ${esc(s)}
      <span class="symptom-tag-remove" onclick="removeSymptom('${esc(s)}')">✕</span>
    </span>`).join('');
}

function populateQuickSymptoms() {
  const container = document.getElementById('quick-symptom-btns');
  container.innerHTML = QUICK_SYMPTOMS.map(s => `
    <button type="button" class="qs-btn" onclick="addQuickSymptom('${s}')">${s}</button>
  `).join('');
}

function addQuickSymptom(s) {
  if (symptoms.includes(s)) return;
  symptoms.push(s);
  renderSymptomTags();
  updatePreview();
}

function resetForm() {
  document.getElementById('triage-form').reset();
  symptoms = [];
  renderSymptomTags();
  document.getElementById('complaint-chars').textContent = '0';
  document.querySelectorAll('.field-error').forEach(e => e.textContent = '');
  document.querySelectorAll('input, select, textarea').forEach(e => {
    e.classList.remove('valid', 'invalid');
  });
  document.querySelectorAll('.vital-indicator').forEach(v => {
    v.className = 'vital-indicator';
  });
  resetPreview();
}

/* ===========================
   FORM VALIDATION
   =========================== */
function validateField(el) {
  const id = el.id;
  const val = el.value.trim();
  let errEl = document.getElementById('err-' + id.replace('f-', ''));
  if (!errEl) return true;
  const v = parseFloat(val);

  const rules = {
    'f-name': [val.length >= 2 && val.length <= 100, 'Name must be 2–100 characters'],
    'f-age': [v >= 0 && v <= 130, 'Age must be 0–130'],
    'f-gender': [['Male', 'Female', 'Other'].includes(val), 'Select Male, Female, or Other'],
    'f-heartrate': [v >= 20 && v <= 300, 'Heart rate: 20–300 bpm'],
    'f-systolicbp': [v >= 50 && v <= 250, 'Systolic BP: 50–250 mmHg'],
    'f-diastolicbp': [v >= 30 && v <= 150, 'Diastolic BP: 30–150 mmHg'],
    'f-temperature': [v >= 30 && v <= 45, 'Temperature: 30–45 °C'],
    'f-resprate': [v >= 4 && v <= 60, 'Respiratory rate: 4–60 br/min'],
    'f-spo2': [v >= 60 && v <= 100, 'O₂ saturation: 60–100%'],
    'f-complaint': [val.length >= 1 && val.length <= 500, 'Chief complaint required (max 500 chars)'],
  };

  if (!val) {
    errEl.textContent = 'This field is required';
    el.classList.add('invalid'); el.classList.remove('valid'); return false;
  }
  if (rules[id]) {
    const [pass, msg] = rules[id];
    if (!pass) {
      errEl.textContent = msg;
      el.classList.add('invalid'); el.classList.remove('valid'); return false;
    }
  }
  errEl.textContent = '';
  el.classList.remove('invalid'); el.classList.add('valid');
  return true;
}

function validateAll() {
  let ok = true;
  document.querySelectorAll('#triage-form input,#triage-form select,#triage-form textarea').forEach(el => {
    if (!validateField(el)) ok = false;
  });
  if (symptoms.length === 0) {
    document.getElementById('err-symptoms').textContent = 'At least one symptom is required';
    ok = false;
  } else {
    document.getElementById('err-symptoms').textContent = '';
  }
  return ok;
}

/* ===========================
   VITAL INDICATORS
   =========================== */
const VITAL_RANGES = {
  'f-heartrate': { ok: [60, 100], warn: [50, 120] },
  'f-systolicbp': { ok: [100, 140], warn: [90, 160] },
  'f-diastolicbp': { ok: [60, 90], warn: [50, 100] },
  'f-temperature': { ok: [36.1, 37.5], warn: [35.5, 39] },
  'f-resprate': { ok: [12, 20], warn: [10, 24] },
  'f-spo2': { ok: [95, 100], warn: [90, 95] },
};

function updateVitalIndicator(id) {
  const el = document.getElementById(id);
  const viId = 'vi-' + id.replace('f-', '');
  const vi = document.getElementById(viId);
  if (!vi || !el) return;
  const v = parseFloat(el.value);
  if (isNaN(v)) { vi.className = 'vital-indicator'; return; }
  const r = VITAL_RANGES[id];
  if (!r) return;
  if (v >= r.ok[0] && v <= r.ok[1]) {
    vi.className = 'vital-indicator vi-normal';
  } else if (v >= r.warn[0] && v <= r.warn[1]) {
    vi.className = 'vital-indicator vi-warning';
  } else {
    vi.className = 'vital-indicator vi-danger';
  }
}

/* ===========================
   LIVE SCORE PREVIEW
   =========================== */
function updatePreview() {
  const hr = parseFloat(document.getElementById('f-heartrate')?.value);
  const sbp = parseFloat(document.getElementById('f-systolicbp')?.value);
  const spo2 = parseFloat(document.getElementById('f-spo2')?.value);
  const rr = parseFloat(document.getElementById('f-resprate')?.value);
  const temp = parseFloat(document.getElementById('f-temperature')?.value);
  const age = parseInt(document.getElementById('f-age')?.value);

  if ([hr, sbp, spo2, rr, temp].some(isNaN)) { resetPreview(); return; }

  let score = 0;

  // HR
  if (hr >= 150 || hr < 40) score += 15;
  else if (hr >= 120 || hr < 50) score += 10;
  else if (hr > 100 || hr < 60) score += 5;
  // SBP
  if (sbp < 70 || sbp > 200) score += 15;
  else if (sbp < 90 || sbp > 180) score += 10;
  else if (sbp < 100 || sbp > 160) score += 5;
  // SpO2
  if (spo2 < 85) score += 15;
  else if (spo2 < 90) score += 10;
  else if (spo2 < 94) score += 5;
  // RR
  if (rr > 30 || rr < 8) score += 10;
  else if (rr > 24 || rr < 10) score += 6;
  else if (rr > 20) score += 3;
  // Temp
  if (temp >= 40 || temp < 35) score += 10;
  else if (temp >= 39 || temp < 35.5) score += 6;
  else if (temp > 37.5) score += 2;

  score = Math.min(score, 40);

  // Symptoms preview
  const symptomScores = {
    'chest pain': 8, 'shortness of breath': 8, 'difficulty breathing': 8,
    'loss of consciousness': 10, 'seizure': 12, 'stroke': 12, 'fever': 4,
    'vomiting': 3, 'dizziness': 4, 'confusion': 5, 'palpitations': 4,
    'numbness': 4, 'weakness': 3, 'headache': 2, 'nausea': 2, 'cough': 1
  };
  let sScore = 0;
  for (const s of symptoms) {
    for (const [key, pts] of Object.entries(symptomScores)) {
      if (s.includes(key)) { sScore += pts; break; }
    }
  }
  score += Math.min(sScore, 30);

  // Age
  if (!isNaN(age)) {
    if (age < 1) score += 15;
    else if (age <= 5) score += 12;
    else if (age >= 80) score += 12;
    else if (age >= 65) score += 8;
    else if (age <= 12) score += 5;
  }

  // Complaint
  const comp = (document.getElementById('f-complaint')?.value || '').toLowerCase();
  const critKw = ['heart attack', 'stroke', 'can\'t breathe', 'not breathing', 'unconscious', 'seizure', 'cardiac', 'overdose'];
  const highKw = ['chest pain', 'chest tightness', 'difficulty breathing', 'shortness of breath', 'severe pain', 'fracture'];
  if (critKw.some(k => comp.includes(k))) score += 15;
  else if (highKw.some(k => comp.includes(k))) score += 10;
  else if (comp.length > 3) score += 2;

  score = Math.min(score, 100);

  const priority = score >= 75 ? 'CRITICAL' : score >= 50 ? 'HIGH' : score >= 25 ? 'MEDIUM' : 'LOW';
  const colors = { CRITICAL: '#DC2626', HIGH: '#EA580C', MEDIUM: '#D97706', LOW: '#16A34A' };
  const descs = {
    CRITICAL: 'Immediate intervention required',
    HIGH: 'Urgent care within 15 minutes',
    MEDIUM: 'Semi-urgent, within 30–60 minutes',
    LOW: 'Non-urgent, can wait'
  };

  const ringFg = document.getElementById('ring-fg');
  const circumference = 2 * Math.PI * 40;
  const dash = (score / 100) * circumference;
  ringFg.style.strokeDasharray = `${dash} ${circumference - dash}`;
  ringFg.style.stroke = colors[priority];

  document.getElementById('ring-score').textContent = score;
  const prev = document.getElementById('preview-priority');
  prev.innerHTML = `<span class="priority-badge badge-${priority}" style="font-size:.82rem">${priority}</span>`;
  document.getElementById('preview-tips').innerHTML = `<p style="color:${colors[priority]};font-weight:600">${descs[priority]}</p>`;
}

function resetPreview() {
  const ringFg = document.getElementById('ring-fg');
  if (ringFg) { ringFg.style.strokeDasharray = '0 251'; ringFg.style.stroke = 'var(--accent)'; }
  const el = document.getElementById('ring-score');
  if (el) el.textContent = '?';
  const prev = document.getElementById('preview-priority');
  if (prev) prev.innerHTML = '<span class="priority-label">—</span>';
  const tips = document.getElementById('preview-tips');
  if (tips) tips.innerHTML = '<p>Fill in patient data to see a live triage estimate</p>';
}

/* ===========================
   FORM SUBMIT
   =========================== */
async function handleSubmit(e) {
  e.preventDefault();
  if (!validateAll()) {
    showToast('Please check all required fields above', 'error');
    return;
  }

  const btn = document.getElementById('btn-submit');
  btn.classList.add('loading');
  btn.innerHTML = '<span class="spin"></span> Submitting...';

  const body = {
    name: document.getElementById('f-name').value.trim(),
    age: parseInt(document.getElementById('f-age').value),
    gender: document.getElementById('f-gender').value,
    heartRate: parseInt(document.getElementById('f-heartrate').value),
    systolicBP: parseInt(document.getElementById('f-systolicbp').value),
    diastolicBP: parseInt(document.getElementById('f-diastolicbp').value),
    temperature: parseFloat(document.getElementById('f-temperature').value),
    respiratoryRate: parseInt(document.getElementById('f-resprate').value),
    oxygenSaturation: parseInt(document.getElementById('f-spo2').value),
    symptoms: [...symptoms],
    chiefComplaint: document.getElementById('f-complaint').value.trim()
  };

  try {
    const res = await fetch(API + '/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    if (!res.ok) {
      const err = await res.json();
      const msg = err.message || err.error || 'Submission failed';
      showToast(msg, 'error');
      return;
    }

    const result = await res.json();
    showResultOverlay(result);
    resetForm();
    showToast('Case submitted successfully!', 'success');
    checkApi();

  } catch (e) {
    showToast('Network error — is the backend running?', 'error');
  } finally {
    btn.classList.remove('loading');
    btn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg> Submit for Triage';
  }
}

/* ===========================
   RESULT OVERLAY
   =========================== */
function showResultOverlay(c) {
  const pl = c.priorityLevel;
  const colors = { CRITICAL: '#FF2D55', HIGH: '#FF7A00', MEDIUM: '#F5C518', LOW: '#06D6A0' };
  const icons = {
    CRITICAL: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>`,
    HIGH: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/></svg>`,
    MEDIUM: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`,
    LOW: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`
  };
  const rationale = (c.priorityRationale || '').split(';').filter(Boolean);

  document.getElementById('result-content').innerHTML = `
    <div class="r-${pl}">
      <div class="result-icon">${icons[pl]}</div>
      <div class="result-title">Triage Complete — ${c.name}</div>
      <div class="result-sub">Assessment completed at ${formatDate(c.submittedAt)}</div>
      <div class="result-score-wrap">
        <div class="result-score" style="color:${colors[pl]}">${c.triageScore}</div>
        <div class="result-score-label">Triage Score / 100</div>
      </div>
      <div class="result-desc badge-${pl}" style="background:rgba(0,0,0,.2);border:1px solid ${colors[pl]}33;color:${colors[pl]}">
        ${pl}: ${esc(c.priorityDescription || '')}
      </div>
      <div class="result-rationale">${rationale.map(r => `• ${r.trim()}`).join('<br/>')}</div>
      <div class="result-btns">
        <button class="btn-secondary" onclick="closeResultOverlay()">Close</button>
        <button class="btn-primary" onclick="closeResultOverlay();navigateTo('cases')">View All Cases</button>
      </div>
    </div>`;

  document.getElementById('result-overlay').classList.add('open');
}

function closeResultOverlay() {
  document.getElementById('result-overlay').classList.remove('open');
}

/* ===========================
   TOASTS
   =========================== */
function showToast(msg, type = 'info') {
  const icons = {
    success: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`,
    error: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`,
    info: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`
  };
  const t = document.createElement('div');
  t.className = `toast toast-${type}`;
  t.innerHTML = `<span class="toast-icon">${icons[type]}</span><span>${esc(msg)}</span>`;
  document.getElementById('toast-container').appendChild(t);
  setTimeout(() => {
    t.classList.add('removing');
    setTimeout(() => t.remove(), 300);
  }, 4000);
}

/* ===========================
   UTILITIES
   =========================== */
function esc(str) {
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function timeAgo(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const diff = (Date.now() - d.getTime()) / 1000;
  if (diff < 60) return 'just now';
  if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
  if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
  return Math.floor(diff / 86400) + 'd ago';
}

function formatDate(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}
