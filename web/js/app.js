/**
 * StudyPoint - Web Hub & One-Click APK Downloader
 * Features: Android Device Detection, Download Automation, QR Code Modal, Search, FAQ Accordion
 */

document.addEventListener('DOMContentLoaded', () => {
  initDeviceDetection();
  initDownloadHandlers();
  initSearch();
  initFaqAccordion();
  initModalHandlers();
});

/**
 * 1. Device Auto-Detection
 */
function initDeviceDetection() {
  const isAndroid = /Android/i.test(navigator.userAgent);
  const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
  
  const deviceBanner = document.getElementById('deviceBanner');
  const deviceStatusText = document.getElementById('deviceStatusText');
  const primaryDownloadBtn = document.getElementById('primaryDownloadBtn');
  const floatingDownloadBtn = document.getElementById('floatingDownloadBtn');
  const qrTriggerBtn = document.getElementById('qrTriggerBtn');

  if (isAndroid) {
    if (deviceBanner) deviceBanner.style.display = 'flex';
    if (deviceStatusText) {
      deviceStatusText.innerHTML = '<strong>Android Device Detected:</strong> Tap below for direct APK installation.';
    }
    if (primaryDownloadBtn) {
      primaryDownloadBtn.classList.add('btn-pulse');
    }
  } else if (!isMobile) {
    // Desktop user
    if (deviceBanner) {
      deviceBanner.style.display = 'flex';
      deviceBanner.style.background = 'linear-gradient(90deg, rgba(79, 70, 229, 0.15) 0%, rgba(6, 182, 212, 0.15) 100%)';
      deviceBanner.style.borderColor = 'rgba(99, 102, 241, 0.3)';
      deviceBanner.style.color = '#c7d2fe';
    }
    if (deviceStatusText) {
      deviceStatusText.innerHTML = '<strong>Browsing on Desktop:</strong> Download for Android Emulator (BlueStacks/LDPlayer) or scan QR code to install on phone.';
    }
    if (qrTriggerBtn) {
      qrTriggerBtn.style.display = 'inline-flex';
    }
  }
}

/**
 * 2. Download Handlers & Toast Notification
 */
function initDownloadHandlers() {
  const downloadButtons = document.querySelectorAll('.trigger-download');
  
  downloadButtons.forEach(btn => {
    btn.addEventListener('click', (e) => {
      // Start download
      showDownloadToast('Downloading StudyPoint APK (v2.4.0)... Please check notifications.');
      
      // Track event if analytics available
      if (typeof gtag === 'function') {
        gtag('event', 'download_apk', {
          'app_name': 'StudyPoint',
          'version': '2.4.0'
        });
      }
    });
  });
}

function showDownloadToast(message) {
  let toast = document.getElementById('downloadToast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'downloadToast';
    toast.style.cssText = `
      position: fixed;
      bottom: 80px;
      right: 20px;
      background: #10b981;
      color: #ffffff;
      padding: 14px 22px;
      border-radius: 12px;
      font-weight: 600;
      font-size: 0.92rem;
      box-shadow: 0 10px 25px rgba(0,0,0,0.5);
      z-index: 9999;
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      transform: translateY(100px);
      opacity: 0;
    `;
    document.body.appendChild(toast);
  }
  
  toast.innerHTML = `
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
      <polyline points="7 10 12 15 17 10"></polyline>
      <line x1="12" y1="15" x2="12" y2="3"></line>
    </svg>
    <span>${message}</span>
  `;
  
  // Animate in
  setTimeout(() => {
    toast.style.transform = 'translateY(0)';
    toast.style.opacity = '1';
  }, 50);

  // Animate out
  setTimeout(() => {
    toast.style.transform = 'translateY(100px)';
    toast.style.opacity = '0';
  }, 4500);
}

/**
 * 3. Instant Search for Subjects & Notes
 */
function initSearch() {
  const searchInput = document.getElementById('searchQuery');
  const classCards = document.querySelectorAll('.class-card');
  const subjectListItems = document.querySelectorAll('.subject-search-item');

  if (!searchInput) return;

  searchInput.addEventListener('input', (e) => {
    const query = e.target.value.toLowerCase().trim();
    if (!query) {
      classCards.forEach(c => c.style.display = 'flex');
      subjectListItems.forEach(s => s.style.display = 'flex');
      return;
    }

    // Filter class cards
    classCards.forEach(card => {
      const text = card.textContent.toLowerCase();
      if (text.includes(query)) {
        card.style.display = 'flex';
      } else {
        card.style.display = 'none';
      }
    });

    // Filter subject items if present on class pages
    subjectListItems.forEach(item => {
      const text = item.textContent.toLowerCase();
      if (text.includes(query)) {
        item.style.display = 'flex';
      } else {
        item.style.display = 'none';
      }
    });
  });
}

/**
 * 4. FAQ Accordion
 */
function initFaqAccordion() {
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const question = item.querySelector('.faq-question');
    if (question) {
      question.addEventListener('click', () => {
        const isActive = item.classList.contains('active');
        // Close others
        faqItems.forEach(other => other.classList.remove('active'));
        if (!isActive) {
          item.classList.add('active');
        }
      });
    }
  });
}

/**
 * 5. Modal Handlers (QR Code & Installation Guide)
 */
function initModalHandlers() {
  const qrTriggerBtn = document.getElementById('qrTriggerBtn');
  const qrModal = document.getElementById('qrModal');
  const installGuideBtn = document.getElementById('installGuideBtn');
  const guideModal = document.getElementById('guideModal');
  const closeButtons = document.querySelectorAll('.modal-close, .modal-overlay');

  if (qrTriggerBtn && qrModal) {
    qrTriggerBtn.addEventListener('click', () => {
      qrModal.classList.add('active');
    });
  }

  if (installGuideBtn && guideModal) {
    installGuideBtn.addEventListener('click', (e) => {
      e.preventDefault();
      guideModal.classList.add('active');
    });
  }

  closeButtons.forEach(btn => {
    btn.addEventListener('click', (e) => {
      if (e.target === btn || btn.classList.contains('modal-close')) {
        document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
      }
    });
  });
}
