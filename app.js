// StudyPoint MCQ Quiz — reads bundled data/mcqs.json
const state = {
  bank: {}, selClass: null, selSubject: null, qCount: 20,
  questions: [], idx: 0, score: 0, answers: []
};

const $ = id => document.getElementById(id);
const show = id => {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  $(id).classList.add('active');
  window.scrollTo(0, 0);
};
const shuffle = a => { for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };

async function init() {
  const res = await fetch('data/mcqs.json');
  state.bank = await res.json();

  const cg = $('class-grid');
  Object.keys(state.bank).sort().forEach(c => {
    const b = document.createElement('button');
    b.className = 'chip'; b.textContent = 'Class ' + c; b.dataset.cls = c;
    b.onclick = () => selectClass(c, b);
    cg.appendChild(b);
  });

  document.querySelectorAll('#count-grid .chip').forEach(b => {
    b.onclick = () => {
      document.querySelectorAll('#count-grid .chip').forEach(x => x.classList.remove('selected'));
      b.classList.add('selected');
      state.qCount = parseInt(b.dataset.count, 10);
      refreshStart();
    };
  });

  $('btn-start').onclick = startQuiz;
  $('btn-next').onclick = nextQuestion;
  $('btn-retry').onclick = () => startQuiz();
  $('btn-home').onclick = () => show('screen-home');

  const total = Object.values(state.bank).reduce((n, q) => n + q.length, 0);
  $('bank-info').textContent = `📚 ${total} MCQs in the bank`;
}

function selectClass(c, btn) {
  state.selClass = c; state.selSubject = null;
  document.querySelectorAll('#class-grid .chip').forEach(x => x.classList.remove('selected'));
  btn.classList.add('selected');
  const subjects = [...new Set(state.bank[c].map(q => q.subject))].sort();
  const sg = $('subject-grid'); sg.innerHTML = '';
  subjects.forEach(s => {
    const n = state.bank[c].filter(q => q.subject === s).length;
    const b = document.createElement('button');
    b.className = 'chip'; b.textContent = `${s} (${n})`;
    b.onclick = () => {
      document.querySelectorAll('#subject-grid .chip').forEach(x => x.classList.remove('selected'));
      b.classList.add('selected');
      state.selSubject = s; refreshStart();
    };
    sg.appendChild(b);
  });
  refreshStart();
}

function refreshStart() {
  const btn = $('btn-start');
  if (state.selClass && state.selSubject) {
    const pool = state.bank[state.selClass].filter(q => q.subject === state.selSubject);
    const n = state.qCount === 0 ? pool.length : Math.min(state.qCount, pool.length);
    btn.disabled = false;
    btn.textContent = `Start Quiz • ${n} questions`;
  } else {
    btn.disabled = true;
    btn.textContent = 'Select class & subject';
  }
}

function startQuiz() {
  const pool = shuffle([...state.bank[state.selClass].filter(q => q.subject === state.selSubject)]);
  const n = state.qCount === 0 ? pool.length : Math.min(state.qCount, pool.length);
  state.questions = pool.slice(0, n);
  state.idx = 0; state.score = 0; state.answers = [];
  show('screen-quiz');
  renderQuestion();
}

function renderQuestion() {
  const q = state.questions[state.idx];
  $('q-counter').textContent = `Q ${state.idx + 1} / ${state.questions.length}`;
  $('q-subject').textContent = `${state.selSubject}`;
  $('progress-fill').style.width = (state.idx / state.questions.length * 100) + '%';
  $('q-chapter').textContent = q.chapter || '';
  $('q-text').textContent = q.question;
  $('explanation').classList.add('hidden');
  $('btn-next').classList.add('hidden');

  const box = $('options'); box.innerHTML = '';
  const keys = ['A', 'B', 'C', 'D'];
  q.options.forEach((opt, i) => {
    const b = document.createElement('button');
    b.className = 'option';
    b.innerHTML = `<span class="key">${keys[i] || ''}</span><span></span>`;
    b.lastChild.textContent = opt;
    b.onclick = () => answer(i, b);
    box.appendChild(b);
  });
}

function answer(i, btn) {
  const q = state.questions[state.idx];
  const opts = [...document.querySelectorAll('#options .option')];
  opts.forEach(o => o.disabled = true);
  const ok = i === q.answer;
  if (ok) state.score++;
  opts[q.answer].classList.add('correct');
  if (!ok) btn.classList.add('wrong');
  state.answers.push({ q, picked: i, ok });
  if (q.explanation) {
    const ex = $('explanation');
    ex.innerHTML = '<b>💡 Explanation:</b> ';
    ex.append(document.createTextNode(q.explanation));
    ex.classList.remove('hidden');
  }
  $('progress-fill').style.width = ((state.idx + 1) / state.questions.length * 100) + '%';
  const nb = $('btn-next');
  nb.textContent = state.idx + 1 === state.questions.length ? 'See Result →' : 'Next →';
  nb.classList.remove('hidden');
}

function nextQuestion() {
  state.idx++;
  if (state.idx >= state.questions.length) return showResult();
  renderQuestion();
}

function showResult() {
  const total = state.questions.length, pct = Math.round(state.score / total * 100);
  $('score-big').textContent = `${state.score}/${total}`;
  $('score-detail').textContent = `${pct}% correct • Class ${state.selClass} • ${state.selSubject}`;
  $('score-fill').style.width = pct + '%';
  $('score-msg').textContent = pct >= 80 ? '🌟 Excellent!' : pct >= 60 ? '👍 Good job!' : pct >= 40 ? '📖 Keep practicing!' : '💪 Don\'t give up — try again!';
  const rl = $('review-list'); rl.innerHTML = '';
  state.answers.forEach(({ q, picked, ok }) => {
    const d = document.createElement('div');
    d.className = 'card';
    const keys = ['A', 'B', 'C', 'D'];
    d.innerHTML = `<p class="review-q">${''}</p>
      <p class="review-a ${ok ? 'ok' : 'bad'}">${ok ? '✅' : '❌'} Your answer: ${(keys[picked] || '') + '. '}</p>
      ${ok ? '' : `<p class="review-a ok">✔ Correct: ${(keys[q.answer] || '') + '. '}</p>`}
      ${q.explanation ? `<p class="muted small" style="margin-top:6px"></p>` : ''}`;
    d.querySelector('.review-q').textContent = q.question;
    d.querySelectorAll('.review-a')[0].append(document.createTextNode(q.options[picked] || ''));
    if (!ok) d.querySelectorAll('.review-a')[1].append(document.createTextNode(q.options[q.answer] || ''));
    const exP = d.querySelector('.muted.small');
    if (exP && q.explanation) exP.textContent = '💡 ' + q.explanation;
    rl.appendChild(d);
  });
  show('screen-result');
}

document.addEventListener('DOMContentLoaded', init);
