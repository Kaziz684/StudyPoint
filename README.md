<div align="center">

# 📚 StudyPoint

**A mobile-friendly MCQ quiz web app for Punjab Board students — practice Class 9–12 questions with instant feedback and explanations.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![HTML5](https://img.shields.io/badge/HTML5-E34F26?logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/HTML)
[![CSS3](https://img.shields.io/badge/CSS3-1572B6?logo=css3&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/CSS)
[![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)

</div>

## ✨ Features

- 🎯 **Class-wise quizzes** — Class 9th, 10th, 11th & 12th (Punjab Board / PCTB syllabus)
- 📖 **Subject filters** — Math, Physics, Chemistry, Computer Science
- ⚡ **Instant feedback** — correct/wrong highlighting with explanations
- 📊 **Score report** — percentage, review of all answers, retry option
- 📱 **Mobile-first design** — readable light theme, big tap targets
- 🔌 **Sheet-powered bank** — 140+ MCQs synced from Google Sheets (`data/mcqs.json`)

## 🚀 Getting Started

No build step, no dependencies — just open it:

```bash
# Clone the repo
git clone https://github.com/Kaziz684/StudyPoint.git
cd StudyPoint

# Serve locally (any static server works)
python3 -m http.server 8000
```

Open [http://localhost:8000](http://localhost:8000) in your browser. You can also
host it free on GitHub Pages, Netlify, or Vercel.

## 📁 Project Structure

```
StudyPoint/
├── index.html      # App shell (home / quiz / result screens)
├── styles.css      # Light, high-contrast readable theme
├── app.js          # Quiz logic (no frameworks)
├── data/
│   └── mcqs.json   # Question bank: id, subject, chapter, question,
│                   #   options[], answer index, explanation
└── README.md
```

## 🔄 Updating the Question Bank

1. Edit the **studypoint mcqs** Google Sheet (tabs: `Class_9th` … `Class_12th`).
   Columns: `id | class_num | subject | chapter | questionText | options | correctAnswerIndex | explanation`
   - `options`: comma-separated; wrap options containing commas in `( )`, `[ ]` or `{ }`
   - `correctAnswerIndex`: 0-based index of the correct option
2. Re-export to `data/mcqs.json` keeping the same schema.
3. Commit & push — the app picks up the new bank automatically.

## 🗺️ Roadmap

- [ ] Timed quiz mode
- [ ] Chapter-wise selection
- [ ] Urdu / Roman-Urdu interface toggle
- [ ] Progress saved in localStorage
- [ ] PWA install support

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
