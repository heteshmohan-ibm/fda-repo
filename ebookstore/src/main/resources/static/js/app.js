/**
 * Book Worm — Hydrangea Floral Pastel Minimalism Design System
 * Bold Purple Accents, Soft Peach Panels, Neutral Book Cards & Sharp Edges
 */

const API_BASE = '/api/v1';

// ── Application State ──
const state = {
  token: localStorage.getItem('bookworm_token') || null,
  user: null,
  cart: { items: [], subtotal: 0 },
  categories: [],
  publishers: [],
  allBooks: [],
  filteredBooks: [],
  selectedCategory: '',
  selectedPublisher: '',
  searchQuery: '',
  sortOrder: 'relevance',
  addresses: [],
  selectedAddressId: null,
  pointsToRedeem: 0,
  paymentMethod: 'CREDIT_CARD',
  currentDetailBook: null,
  lastOrder: null,
  lastPayment: null,
  debounceTimer: null
};

// ── Dynamic 3D Book Cover Palette (Rich tones that pop on neutral cards) ──
const PALETTE = [
  { bg: 'linear-gradient(135deg, #3A1046 0%, #5E1F73 60%, #74318A 100%)', text: '#ffffff', tag: '#FF9CE9' },
  { bg: 'linear-gradient(135deg, #064E3B 0%, #047857 60%, #059669 100%)', text: '#ffffff', tag: '#A7F3D0' },
  { bg: 'linear-gradient(135deg, #701A75 0%, #A21CAF 60%, #C026D3 100%)', text: '#ffffff', tag: '#F5D0FE' },
  { bg: 'linear-gradient(135deg, #7C2D12 0%, #C2410C 60%, #EA580C 100%)', text: '#ffffff', tag: '#FFC2BA' },
  { bg: 'linear-gradient(135deg, #0C4A6E 0%, #0284C7 60%, #38BDF8 100%)', text: '#ffffff', tag: '#BAE6FD' },
  { bg: 'linear-gradient(135deg, #831843 0%, #BE185D 60%, #DB2777 100%)', text: '#ffffff', tag: '#FF8DA1' },
  { bg: 'linear-gradient(135deg, #1E1B4B 0%, #312E81 60%, #4338CA 100%)', text: '#ffffff', tag: '#C7D2FE' }
];

function getCoverStyle(title) {
  let hash = 0;
  for (let i = 0; i < (title || '').length; i++) {
    hash = (hash << 5) - hash + title.charCodeAt(i);
  }
  return PALETTE[Math.abs(hash) % PALETTE.length];
}

// ── API Fetch Client ──
async function api(endpoint, options = {}) {
  const headers = options.headers || {};
  if (!headers['Content-Type'] && !(options.body instanceof FormData) && options.method !== 'DELETE') {
    headers['Content-Type'] = 'application/json';
  }
  if (state.token) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }

  const res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);

  if (!res.ok) {
    const errorMsg = data?.message || data?.error || `Request failed (${res.status})`;
    const err = new Error(errorMsg);
    err.status = res.status;
    err.code = data?.code;
    throw err;
  }
  return data;
}

// ── Toast Notifications (Hydrangea Floral Theme) ──
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  
  let borderClass = 'border-[#AD56C4] text-[#1E2024]';
  let icon = '🌸';
  if (type === 'success') {
    borderClass = 'border-emerald-600 text-emerald-950';
    icon = '✓';
  } else if (type === 'error') {
    borderClass = 'border-[#FF8DA1] text-[#1E2024]';
    icon = '✕';
  }

  toast.className = `pointer-events-auto flex items-center gap-3 px-4 py-3 bg-white/95 border-2 ${borderClass} shadow-xl backdrop-blur-xs text-xs font-semibold transition-all duration-300 transform translate-y-2 opacity-0`;
  toast.innerHTML = `<span class="text-sm font-black">${icon}</span><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  // Trigger animation
  requestAnimationFrame(() => {
    toast.classList.remove('translate-y-2', 'opacity-0');
  });

  setTimeout(() => {
    toast.classList.add('translate-y-2', 'opacity-0');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// ── Navigation Router ──
function navigateTo(viewId) {
  document.querySelectorAll('.view-pane').forEach(v => v.classList.remove('active'));
  
  // Header links active styling
  const navBrowse = document.getElementById('nav-catalogue');
  const navOrders = document.getElementById('nav-orders');
  if (navBrowse && navOrders) {
    if (viewId === 'catalogue') {
      navBrowse.className = 'text-sm font-bold text-[#74318A] relative py-1 border-b-2 border-[#AD56C4] transition-colors';
      navOrders.className = 'text-sm font-semibold text-[#4A4F59] hover:text-[#74318A] transition-colors relative py-1';
    } else if (viewId === 'orders') {
      navOrders.className = 'text-sm font-bold text-[#74318A] relative py-1 border-b-2 border-[#AD56C4] transition-colors';
      navBrowse.className = 'text-sm font-semibold text-[#4A4F59] hover:text-[#74318A] transition-colors relative py-1';
    } else {
      navBrowse.className = 'text-sm font-semibold text-[#4A4F59] hover:text-[#74318A] transition-colors relative py-1';
      navOrders.className = 'text-sm font-semibold text-[#4A4F59] hover:text-[#74318A] transition-colors relative py-1';
    }
  }

  const target = document.getElementById(`view-${viewId}`);
  if (target) target.classList.add('active');

  // Toggle filter toolbar visibility (only in catalogue browse)
  const toolbar = document.getElementById('filter-toolbar');
  if (toolbar) {
    toolbar.style.display = (viewId === 'catalogue') ? 'block' : 'none';
  }

  window.scrollTo({ top: 0, behavior: 'smooth' });

  if (viewId === 'catalogue') {
    loadBooks();
  } else if (viewId === 'cart') {
    renderCartView();
  } else if (viewId === 'orders') {
    loadOrdersHistory();
  }
}

// ── Authentication & Profile Management ──
async function initAuth() {
  if (!state.token) {
    await loginDemo(true);
  } else {
    try {
      await refreshProfile();
      await fetchCart();
    } catch (e) {
      console.warn('Token expired or invalid, re-authenticating demo user...', e);
      await loginDemo(true);
    }
  }
}

async function loginDemo(silent = false) {
  try {
    const res = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'demo@ebookstore.com', password: 'demo1234' })
    });
    state.token = res.token;
    localStorage.setItem('bookworm_token', res.token);
    await refreshProfile();
    await fetchCart();
    closeAuthModal();
    if (!silent) showToast(`Signed in as ${res.user.name}`, 'success');
  } catch (err) {
    console.error('Demo sign-in error:', err);
    if (!silent) showToast('Sign-in failed: ' + err.message, 'error');
  }
}

async function handleAuthSubmit(e) {
  e.preventDefault();
  const email = document.getElementById('auth-email-input').value;
  const password = document.getElementById('auth-pass-input').value;
  try {
    const res = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    state.token = res.token;
    localStorage.setItem('bookworm_token', res.token);
    await refreshProfile();
    await fetchCart();
    closeAuthModal();
    showToast(`Welcome back, ${res.user.name}!`, 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function refreshProfile() {
  try {
    const user = await api('/me');
    state.user = user;
    document.getElementById('points-display').textContent = user.giftPointsBalance;
    document.getElementById('checkout-avail-pts').textContent = user.giftPointsBalance;
    document.getElementById('user-name-display').textContent = user.name;
    document.getElementById('user-avatar-char').textContent = user.name.charAt(0).toUpperCase();
  } catch (err) {
    console.error('Profile fetch error:', err);
  }
}

function openAuthModal() {
  const m = document.getElementById('modal-auth');
  m.classList.remove('hidden');
  m.classList.add('flex');
}
function closeAuthModal() {
  const m = document.getElementById('modal-auth');
  m.classList.add('hidden');
  m.classList.remove('flex');
}

// ── Catalogue Metadata (Categories & Publishers) ──
async function loadCategories() {
  try {
    state.categories = await api('/categories');
  } catch (err) {
    console.error('Category load error:', err);
  }
}

async function loadPublishers() {
  try {
    state.publishers = await api('/publishers');
    const select = document.getElementById('filter-publisher');
    select.innerHTML = '<option value="">All Publishers / Brands</option>';
    state.publishers.forEach(p => {
      const opt = document.createElement('option');
      opt.value = p;
      opt.textContent = p;
      select.appendChild(opt);
    });
  } catch (err) {
    console.error('Publisher load error:', err);
  }
}

async function loadBooks() {
  try {
    const data = await api('/books?size=50');
    state.allBooks = data.content || [];
    document.getElementById('cat-all-count').textContent = state.allBooks.length;
    applyFilters();
    loadRecommendations();
  } catch (err) {
    showToast('Failed to load books: ' + err.message, 'error');
  }
}

// ── Sidebar Category Navigation ──
function selectCategory(categoryName) {
  state.selectedCategory = categoryName;

  const buttons = document.querySelectorAll('#sidebar-categories button');
  buttons.forEach(btn => {
    const span = btn.querySelector('span');
    const text = span ? span.textContent.trim() : '';

    if ((categoryName === '' && btn.id === 'cat-all') || (categoryName && text.toLowerCase().includes(categoryName.toLowerCase()))) {
      btn.className = 'w-full flex items-center justify-between px-3 py-2 bg-[#FFEAE6] text-[#74318A] font-bold border-l-4 border-[#AD56C4] transition-colors text-left';
    } else {
      btn.className = 'w-full flex items-center justify-between px-3 py-2 text-[#4A4F59] hover:text-[#1E2024] hover:bg-[#FFEAE6]/60 transition-colors font-medium text-left';
    }
  });

  navigateTo('catalogue');
  applyFilters();
}

function handleSearchDebounce() {
  clearTimeout(state.debounceTimer);
  state.debounceTimer = setTimeout(() => {
    state.searchQuery = document.getElementById('search-input').value.trim();
    applyFilters();
  }, 250);
}

function handleSortChange() {
  state.sortOrder = document.getElementById('sort-select').value;
  applyFilters();
}

function applyFilters() {
  state.selectedPublisher = document.getElementById('filter-publisher').value;

  let list = [...state.allBooks];

  // Category filter
  if (state.selectedCategory) {
    const targetCat = state.categories.find(c => c.name.toLowerCase().includes(state.selectedCategory.toLowerCase()));
    if (targetCat) {
      list = list.filter(b => b.categoryId === targetCat.id);
    }
  }

  // Publisher brand filter
  if (state.selectedPublisher) {
    list = list.filter(b => b.publisher === state.selectedPublisher);
  }

  // Search text filter
  if (state.searchQuery) {
    const q = state.searchQuery.toLowerCase();
    list = list.filter(b => b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q) || (b.description && b.description.toLowerCase().includes(q)));
  }

  // Sorting
  if (state.sortOrder === 'price-asc') {
    list.sort((a, b) => Number(a.price) - Number(b.price));
  } else if (state.sortOrder === 'price-desc') {
    list.sort((a, b) => Number(b.price) - Number(a.price));
  }

  state.filteredBooks = list;

  // Render main catalogue grid
  renderSplitBookGrid(list, 'grid-catalogue');

  // Render New Launches
  const newLaunches = [...state.allBooks].reverse().slice(0, 3);
  renderSplitBookGrid(newLaunches, 'grid-new-launches');
}

// ── Render Split Book Cards (Mostly Neutral so Covers Pop) ──
function renderSplitBookGrid(books, containerId) {
  const container = document.getElementById(containerId);
  container.innerHTML = '';

  if (!books || books.length === 0) {
    container.innerHTML = `
      <div class="col-span-full py-12 text-center text-[#717786]">
        <p class="text-sm">No books match your current criteria.</p>
      </div>`;
    return;
  }

  books.forEach(b => {
    const card = document.createElement('div');
    card.className = 'group bg-white border border-[#E8E2DC] hover:border-[#AD56C4] transition-all duration-300 p-4 flex gap-4 shadow-xs hover:shadow-md hover:-translate-y-0.5';

    const coverStyle = getCoverStyle(b.title);
    const cat = state.categories.find(c => c.id === b.categoryId);
    const catName = cat ? cat.name : 'Non-fiction';

    card.innerHTML = `
      <!-- Left: 3D Book Cover -->
      <div class="book-cover-3d w-28 h-40 shrink-0 p-3 flex flex-col justify-end text-white cursor-pointer relative shadow-sm" style="background: ${coverStyle.bg}" onclick="openBookDetails(${b.id})">
        <div class="book-spine-crease"></div>
        <div class="book-pattern"></div>
        <div class="relative z-10">
          <div class="text-[11px] font-extrabold leading-tight drop-shadow-md line-clamp-2">${escapeHtml(b.title)}</div>
          <div class="text-[9px] text-slate-100 mt-0.5 drop-shadow truncate">${escapeHtml(b.author)}</div>
        </div>
      </div>

      <!-- Right: Metadata & Actions -->
      <div class="flex-1 flex flex-col justify-between min-w-0">
        <div>
          <h3 class="text-sm font-bold text-[#1E2024] group-hover:text-[#74318A] transition-colors line-clamp-1 cursor-pointer" onclick="openBookDetails(${b.id})">
            ${escapeHtml(b.title)}
          </h3>
          <p class="text-xs text-[#74318A] hover:text-[#5C236E] transition-colors font-medium mt-0.5 cursor-pointer truncate" onclick="openBookDetails(${b.id})">
            by ${escapeHtml(b.author)}
          </p>
          <p class="text-xs text-[#4A4F59] line-clamp-2 mt-1.5 leading-relaxed">
            ${escapeHtml(b.description || 'Essential reading curated by specialists.')}
          </p>
          <div class="flex items-center gap-1.5 mt-2">
            <span class="text-[10px] font-medium text-[#717786]">Paperback &bull;</span>
            <span class="text-[10px] font-bold text-[#74318A] bg-[#FFF0FC] border border-[#FF9CE9] px-1.5 py-0.5">${escapeHtml(catName)}</span>
          </div>
        </div>

        <div class="pt-3 border-t border-[#E8E2DC] mt-2">
          <div class="flex items-baseline justify-between mb-1">
            <div class="text-base font-black text-[#1E2024]">₹${Number(b.price).toFixed(2)}</div>
            <div class="text-[10px] text-[#74318A] font-bold bg-[#FFEAE6] px-1.5 py-0.5 border border-[#FFC2BA]/60">Delivery Mon, 21 Jul</div>
          </div>
          <div class="flex items-center gap-2 mt-1.5">
            <button class="flex-1 py-1.5 px-3 bg-[#74318A] hover:bg-[#5C236E] active:scale-95 disabled:opacity-40 disabled:pointer-events-none text-white text-xs font-bold transition-all flex items-center justify-center gap-1 shadow-xs cursor-pointer" ${b.availableStock === 0 ? 'disabled' : ''} onclick="quickAddToCart(${b.id}, event)">
              <span>🛒 Add to Cart</span>
            </button>
            <button class="py-1.5 px-2.5 bg-[#FAF8F5] hover:bg-[#FFEAE6] text-[#1E2024] text-xs font-semibold border border-[#E8E2DC] transition-colors cursor-pointer" onclick="openBookDetails(${b.id})">
              Details
            </button>
          </div>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

// ── Recommendations Shelf (Slides 5 & 7) ──
async function loadRecommendations() {
  try {
    const recs = await api('/me/recommendations?size=3');
    if (!recs || recs.length === 0) {
      renderSplitBookGrid(state.allBooks.slice(0, 3), 'grid-recommended');
    } else {
      renderSplitBookGrid(recs, 'grid-recommended');
    }
  } catch (err) {
    renderSplitBookGrid(state.allBooks.slice(0, 3), 'grid-recommended');
  }
}

// ── Book Details View (Slide 7 - Dual Cover with ISBN Barcode) ──
async function openBookDetails(bookId) {
  try {
    const book = await api(`/books/${bookId}`);
    state.currentDetailBook = book;

    const cat = state.categories.find(c => c.id === book.categoryId);
    const catName = cat ? cat.name : 'Non-fiction';

    document.getElementById('crumbs-category').textContent = catName;
    document.getElementById('crumbs-title').textContent = book.title;

    document.getElementById('details-title').textContent = book.title;
    document.getElementById('details-author').textContent = `by ${book.author}`;
    document.getElementById('details-publisher').textContent = book.publisher;
    document.getElementById('details-category-pill').textContent = catName;
    document.getElementById('details-price').textContent = `₹${Number(book.price).toFixed(2)}`;
    document.getElementById('details-description').textContent = book.description || 'Discover insightful takeaways, deep narrative structure, and actionable principles.';
    document.getElementById('writer-name').textContent = book.author;

    // Stock pill
    const stockPill = document.getElementById('details-stock-status');
    const addBtn = document.getElementById('btn-details-add');
    if (book.availableStock === 0) {
      stockPill.className = 'px-2.5 py-0.5 bg-rose-50 border border-rose-300 text-rose-800 text-xs font-bold';
      stockPill.textContent = 'Out of Stock';
      addBtn.disabled = true;
    } else {
      stockPill.className = 'px-2.5 py-0.5 bg-emerald-50 border border-emerald-300 text-emerald-800 text-xs font-bold';
      stockPill.textContent = `In Stock (${book.availableStock} available)`;
      addBtn.disabled = false;
    }

    // 3D Front & Back Cover Display (Slide 7 - image10.png)
    const coverStyle = getCoverStyle(book.title);
    const coverBox = document.getElementById('details-cover-box');
    if (coverBox) {
      coverBox.style.background = coverStyle.bg;
      document.getElementById('details-cover-title').textContent = book.title;
      document.getElementById('details-cover-author').textContent = `by ${book.author}`;
    }

    // Dynamic Back Cover styling & metadata
    const backBox = document.getElementById('details-back-cover-box');
    if (backBox) {
      backBox.style.background = coverStyle.bg;
    }

    // Quotes for back cover (Slide 7 wireframe image10.png)
    const QUOTES_MAP = {
      'The Joy of Minimalism': '"A refreshing path to clarity, focus, and mastery in a complex world."',
      'The Path to Success': '"Success is not an accident—it is an intentional daily craft."',
      'The Art of Focus': '"Where deep attention flows, breakthrough innovation and mastery follow."',
      'Atomic Habits': '"Small changes create remarkable results. Master systems over goals."',
      'Clean Code': '"Clean code always looks like it was written by someone who cares."',
      'Sapiens': '"We study history not to know the future, but to widen our horizons."',
      'The Great Gatsby': '"So we beat on, boats against the current, borne back ceaselessly into the past."',
      '1984': '"Freedom is the freedom to say that two plus two make four."'
    };
    const quote = QUOTES_MAP[book.title] ||
      (book.description ? `"${book.description.split('.')[0]}."` : '"A transformative reading experience for clarity and mastery."');
    const backQuoteEl = document.getElementById('details-back-quote');
    if (backQuoteEl) backQuoteEl.textContent = quote;

    // Blurb excerpt for back cover
    const backBlurbEl = document.getElementById('details-back-blurb');
    if (backBlurbEl) {
      backBlurbEl.textContent = book.description ||
        'Guides you through practical strategies to declutter your mind, space, and workflow with a calm, mindful approach.';
    }

    // Author bio snippet
    const AUTHOR_BIOS = {
      'Rachel Green': 'Rachel Green is a renowned minimalist practitioner, designer, and mindfulness educator whose principles have helped over 100,000 readers worldwide find clarity.',
      'Michael Scott': 'Michael Scott is a seasoned leadership strategist and writer focusing on sustainable personal mastery.',
      'David Kim': 'David Kim is a cognitive scientist and researcher exploring deep focus and creative flow in high-stakes environments.',
      'James Clear': 'James Clear is an author and speaker whose work on habit formation and continuous improvement is read by millions.',
      'Robert C. Martin': 'Robert C. Martin ("Uncle Bob") is an internationally acclaimed software craftsman and author.'
    };
    const authorBio = AUTHOR_BIOS[book.author] ||
      `${book.author} is a distinguished specialist and author whose insights have helped thousands of readers build sustainable understanding and habits.`;
    const backBioEl = document.getElementById('details-back-author-bio');
    if (backBioEl) backBioEl.textContent = authorBio;

    // Publisher & Colophon Logo
    const publisherName = book.publisher || 'ABC Publishers';
    const backPubEl = document.getElementById('details-back-publisher');
    if (backPubEl) backPubEl.textContent = publisherName;
    const colophonLetterEl = document.getElementById('details-back-colophon-letter');
    if (colophonLetterEl) colophonLetterEl.textContent = (publisherName.charAt(0) || 'Q').toUpperCase();

    // ISBN-13 & Barcode (Slide 7 wireframe: ISBN 978-0-123456-78-9)
    let isbnText, digitsText;
    if (book.title === 'The Joy of Minimalism') {
      isbnText = 'ISBN 978-0-123456-78-9';
      digitsText = '9 780123 456789';
    } else {
      const g1 = String(100000 + (book.id * 31415) % 899999).slice(0, 6);
      const g2 = String(10 + (book.id * 17) % 89).slice(0, 2);
      const chk = (book.id * 7 + 1) % 10;
      isbnText = `ISBN 978-0-${g1}-${g2}-${chk}`;
      digitsText = `9 780${g1.slice(0, 3)} ${g1.slice(3)}${g2}${chk}`;
    }
    const barcodeIsbnEl = document.getElementById('details-barcode-isbn');
    if (barcodeIsbnEl) barcodeIsbnEl.textContent = isbnText;
    const barcodeDigitsEl = document.getElementById('details-barcode-digits');
    if (barcodeDigitsEl) barcodeDigitsEl.textContent = digitsText;

    // Load related reads
    loadRelatedReads(book.id);

    navigateTo('details');
  } catch (err) {
    showToast('Failed to load book: ' + err.message, 'error');
  }
}

async function loadRelatedReads(bookId) {
  const container = document.getElementById('details-related-reads');
  container.innerHTML = '<span class="text-xs text-[#717786]">Loading related reads...</span>';
  try {
    const related = await api(`/books/${bookId}/related`);
    container.innerHTML = '';
    if (!related || related.length === 0) {
      container.innerHTML = '<span class="text-xs text-[#717786]">No other books in this category.</span>';
      return;
    }
    related.slice(0, 3).forEach(r => {
      const item = document.createElement('div');
      item.className = 'p-3 bg-white hover:bg-[#FFF5F3] border border-[#E8E2DC] hover:border-[#AD56C4] transition-colors cursor-pointer flex gap-3 items-center shadow-xs';
      item.onclick = () => openBookDetails(r.id);

      const coverStyle = getCoverStyle(r.title);
      item.innerHTML = `
        <div class="book-cover-3d w-12 h-16 shrink-0 p-1.5 flex flex-col justify-end text-white shadow-xs" style="background: ${coverStyle.bg}">
          <div class="book-spine-crease"></div>
          <div class="text-[8px] font-extrabold leading-tight line-clamp-2">${escapeHtml(r.title)}</div>
        </div>
        <div class="flex-1 min-w-0">
          <div class="text-xs font-bold text-[#1E2024] truncate">${escapeHtml(r.title)}</div>
          <div class="text-[11px] text-[#74318A] truncate font-medium">by ${escapeHtml(r.author)}</div>
          <div class="text-xs font-black text-[#1E2024] mt-1">₹${Number(r.price).toFixed(2)}</div>
        </div>
      `;
      container.appendChild(item);
    });
  } catch (err) {
    container.innerHTML = '';
  }
}

async function addCurrentBookToCart() {
  if (!state.currentDetailBook) return;
  await quickAddToCart(state.currentDetailBook.id, null, 1);
}

// ── Shopping Cart & Checkout View (Slide 8 - image11.png) ──
async function fetchCart() {
  try {
    const cart = await api('/me/cart');
    state.cart = cart;
    updateCartCounter();
  } catch (err) {
    console.error('Cart fetch error:', err);
  }
}

function updateCartCounter() {
  const totalQty = (state.cart.items || []).reduce((acc, i) => acc + i.quantity, 0);
  document.getElementById('cart-badge').textContent = totalQty;
  document.getElementById('cart-item-count-label').textContent = totalQty;
  document.getElementById('calc-qty-label').textContent = totalQty;
}

async function quickAddToCart(bookId, e, quantity = 1) {
  if (e) e.stopPropagation();
  try {
    const cart = await api('/me/cart/items', {
      method: 'POST',
      body: JSON.stringify({ bookId, quantity })
    });
    state.cart = cart;
    updateCartCounter();
    showToast('Added to your basket!', 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function updateCartItemQuantity(bookId, quantity) {
  if (quantity <= 0) {
    await api(`/me/cart/items/${bookId}`, { method: 'DELETE' });
  } else {
    await api(`/me/cart/items/${bookId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity })
    });
  }
  await fetchCart();
  renderCartView();
}

async function renderCartView() {
  await fetchCart();
  await refreshProfile();
  await loadAddresses();

  const emptyNotice = document.getElementById('cart-empty-notice');
  const nonEmpty = document.getElementById('cart-non-empty');
  const shelf = document.getElementById('cart-items-shelf');

  if (!state.cart.items || state.cart.items.length === 0) {
    emptyNotice.classList.remove('hidden');
    nonEmpty.classList.add('hidden');
    return;
  }

  emptyNotice.classList.add('hidden');
  nonEmpty.classList.remove('hidden');
  shelf.innerHTML = '';

  state.cart.items.forEach(item => {
    const coverStyle = getCoverStyle(item.book.title);
    const card = document.createElement('div');
    card.className = 'p-3.5 bg-white border border-[#E8E2DC] flex gap-3 items-center shadow-xs';
    card.innerHTML = `
      <div class="book-cover-3d w-14 h-20 shrink-0 p-1.5 flex flex-col justify-end text-white shadow-xs" style="background: ${coverStyle.bg}">
        <div class="book-spine-crease"></div>
        <div class="text-[9px] font-extrabold leading-tight line-clamp-2">${escapeHtml(item.book.title)}</div>
      </div>
      <div class="flex-1 min-w-0">
        <h4 class="text-xs font-bold text-[#1E2024] truncate">${escapeHtml(item.book.title)}</h4>
        <p class="text-[11px] text-[#74318A] truncate font-medium">by ${escapeHtml(item.book.author)}</p>
        <div class="flex items-center justify-between mt-2">
          <span class="text-xs font-black text-[#1E2024]">₹${Number(item.book.price).toFixed(2)}</span>
          <div class="flex items-center gap-1.5 bg-[#FAF8F5] border border-[#E8E2DC] px-2 py-0.5">
            <button class="text-xs font-bold text-[#4A4F59] hover:text-[#74318A] cursor-pointer" onclick="updateCartItemQuantity(${item.book.id}, ${item.quantity - 1})">-</button>
            <span class="text-xs font-bold text-[#1E2024] px-1">${item.quantity}</span>
            <button class="text-xs font-bold text-[#4A4F59] hover:text-[#74318A] cursor-pointer" onclick="updateCartItemQuantity(${item.book.id}, ${item.quantity + 1})">+</button>
          </div>
          <button class="text-[11px] text-rose-600 hover:text-rose-800 font-semibold cursor-pointer" onclick="updateCartItemQuantity(${item.book.id}, 0)">
            Remove
          </button>
        </div>
      </div>
    `;
    shelf.appendChild(card);
  });

  const subtotal = Number(state.cart.subtotal || 0);
  const discount = Math.min(state.pointsToRedeem, Math.floor(subtotal));
  const finalTotal = Math.max(0, subtotal - discount);

  document.getElementById('calc-subtotal').textContent = `₹${subtotal.toFixed(2)}`;
  document.getElementById('calc-discount').textContent = `-₹${discount.toFixed(2)}`;
  document.getElementById('calc-final-total').textContent = `₹${finalTotal.toFixed(2)}`;
  document.getElementById('redeem-points-input').value = state.pointsToRedeem;
  document.getElementById('redeem-points-input').max = Math.min(state.user?.giftPointsBalance || 0, Math.floor(subtotal));
}

// ── Address Selection & Form (Slide 8) ──
async function loadAddresses() {
  try {
    state.addresses = await api('/me/addresses');
    const dropdown = document.getElementById('saved-address-dropdown');
    dropdown.innerHTML = '';

    if (state.addresses && state.addresses.length > 0) {
      if (!state.selectedAddressId) {
        state.selectedAddressId = state.addresses[0].id;
      }
      state.addresses.forEach(a => {
        const opt = document.createElement('option');
        opt.value = a.id;
        opt.textContent = `${a.recipient} (${a.city} - ${a.postalCode})`;
        if (a.id === state.selectedAddressId) opt.selected = true;
        dropdown.appendChild(opt);
      });
      fillAddressForm(state.addresses.find(a => a.id === state.selectedAddressId));
    }
  } catch (err) {
    console.error('Address load error:', err);
  }
}

function handleAddressSelect(addressId) {
  state.selectedAddressId = parseInt(addressId, 10);
  const addr = state.addresses.find(a => a.id === state.selectedAddressId);
  if (addr) fillAddressForm(addr);
}

function fillAddressForm(addr) {
  if (!addr) return;
  document.getElementById('addr-first-name').value = addr.recipient.split(' ')[0] || 'Demo';
  document.getElementById('addr-last-name').value = addr.recipient.split(' ').slice(1).join(' ') || 'User';
  document.getElementById('addr-line').value = `${addr.line1}${addr.line2 ? ', ' + addr.line2 : ''}`;
  document.getElementById('addr-city').value = addr.city;
  document.getElementById('addr-pin').value = addr.postalCode;
  document.getElementById('addr-state').value = addr.state;
}

function toggleSavedAddress(useSaved) {
  const dropdown = document.getElementById('saved-address-dropdown');
  dropdown.disabled = !useSaved;
  if (useSaved && state.addresses.length > 0) {
    fillAddressForm(state.addresses.find(a => a.id === state.selectedAddressId));
  }
}

// ── Gift Points Redemption ──
function applyPointsChange(val) {
  const points = parseInt(val, 10) || 0;
  const maxPts = Math.min(state.user?.giftPointsBalance || 0, Math.floor(state.cart.subtotal || 0));
  state.pointsToRedeem = Math.max(0, Math.min(points, maxPts));
  renderCartTotals();
}

function applyPoints() {
  const input = document.getElementById('redeem-points-input');
  applyPointsChange(input.value);
  showToast(`Applied ${state.pointsToRedeem} Gift Points discount`, 'success');
}

function renderCartTotals() {
  const subtotal = Number(state.cart.subtotal || 0);
  const discount = Math.min(state.pointsToRedeem, Math.floor(subtotal));
  const finalTotal = Math.max(0, subtotal - discount);

  document.getElementById('calc-subtotal').textContent = `₹${subtotal.toFixed(2)}`;
  document.getElementById('calc-discount').textContent = `-₹${discount.toFixed(2)}`;
  document.getElementById('calc-final-total').textContent = `₹${finalTotal.toFixed(2)}`;
}

// ── Payment Modal (Slide 9 - image12.png) ──
function openPaymentModal() {
  if (!state.cart.items || state.cart.items.length === 0) {
    showToast('Your basket is empty!', 'error');
    return;
  }
  const modal = document.getElementById('modal-payment');
  const subtotal = Number(state.cart.subtotal || 0);
  const discount = Math.min(state.pointsToRedeem, Math.floor(subtotal));
  const payable = Math.max(0, subtotal - discount);

  document.getElementById('payment-modal-amount').textContent = `₹${payable.toFixed(2)}`;
  modal.classList.remove('hidden');
  modal.classList.add('flex');
}

function closePaymentModal() {
  const modal = document.getElementById('modal-payment');
  modal.classList.add('hidden');
  modal.classList.remove('flex');
}

function selectPayTab(method, btn) {
  state.paymentMethod = method;
  document.querySelectorAll('#modal-payment button[onclick^="selectPayTab"]').forEach(b => {
    b.className = 'w-full text-left px-3 py-2.5 text-xs font-semibold text-[#4A4F59] hover:text-[#1E2024] hover:bg-white transition-colors';
  });
  btn.className = 'w-full text-left px-3 py-2.5 text-xs font-bold text-[#74318A] bg-[#FFEAE6] border-l-4 border-[#AD56C4] shadow-xs';
}

async function executePaymentTransaction() {
  const btn = document.getElementById('btn-execute-payment');
  btn.disabled = true;
  btn.textContent = 'Processing Transaction...';

  const outcomeSelect = document.getElementById('payment-outcome-select');
  const simulateOutcome = outcomeSelect ? outcomeSelect.value : 'APPROVED';

  const orderIdempotencyKey = generateUUID();
  const payIdempotencyKey = generateUUID();

  try {
    // 1. Create Order with Idempotency Key
    const order = await api('/me/orders', {
      method: 'POST',
      headers: { 'Idempotency-Key': orderIdempotencyKey },
      body: JSON.stringify({
        addressId: state.selectedAddressId || (state.addresses[0] ? state.addresses[0].id : 1),
        pointsToRedeem: state.pointsToRedeem
      })
    });

    // 2. Pay Order with Idempotency Key & Simulated Outcome
    const payment = await api(`/me/orders/${order.id}/pay`, {
      method: 'POST',
      headers: { 'Idempotency-Key': payIdempotencyKey },
      body: JSON.stringify({
        method: state.paymentMethod,
        simulateOutcome: simulateOutcome
      })
    });

    state.lastOrder = order;
    state.lastPayment = payment;

    closePaymentModal();
    renderConfirmationModal(order, payment);

    await refreshProfile();
    await fetchCart();
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Pay Now</span><span>💳</span>';
  }
}

// ── Purchase Confirmation Modal (Slide 10 - image13.png) ──
function renderConfirmationModal(order, payment) {
  const modal = document.getElementById('modal-confirmation');
  const icon = document.getElementById('confirm-icon');
  const heading = document.getElementById('confirm-heading');
  const shelf = document.getElementById('confirm-books-container');

  shelf.innerHTML = '';

  if (payment.paymentStatus === 'APPROVED') {
    icon.className = 'w-16 h-16 bg-[#FFEAE6] border-2 border-[#AD56C4] flex items-center justify-center text-[#74318A] text-2xl mx-auto mb-4 font-black';
    icon.textContent = '✓';
    heading.textContent = 'Your purchase of the following reads is successful';
  } else {
    icon.className = 'w-16 h-16 bg-rose-50 border-2 border-rose-400 flex items-center justify-center text-rose-600 text-2xl mx-auto mb-4 font-black';
    icon.textContent = '✕';
    heading.textContent = 'Payment Simulation Declined (Order Marked PAYMENT_FAILED)';
  }

  (order.items || []).forEach(item => {
    const coverStyle = getCoverStyle(item.title);
    const card = document.createElement('div');
    card.className = 'p-3 bg-white border border-[#E8E2DC] flex gap-3 text-left w-64 shadow-xs';
    card.innerHTML = `
      <div class="book-cover-3d w-14 h-20 shrink-0 p-1.5 flex flex-col justify-end text-white shadow-xs" style="background: ${coverStyle.bg}">
        <div class="book-spine-crease"></div>
        <div class="text-[9px] font-extrabold leading-tight line-clamp-2">${escapeHtml(item.title)}</div>
      </div>
      <div class="flex-1 min-w-0">
        <h4 class="text-xs font-bold text-[#1E2024] truncate">${escapeHtml(item.title)}</h4>
        <div class="text-[10px] text-[#717786] mt-0.5">Qty: ${item.quantity}</div>
        <div class="text-xs font-black text-[#1E2024] mt-1">₹${Number(item.lineTotal).toFixed(2)}</div>
        <div class="text-[10px] text-[#74318A] font-bold mt-1">Delivery by Mon, 21 Jul</div>
      </div>
    `;
    shelf.appendChild(card);
  });

  modal.classList.remove('hidden');
  modal.classList.add('flex');
}

function continueShopping() {
  const m = document.getElementById('modal-confirmation');
  m.classList.add('hidden');
  m.classList.remove('flex');
  navigateTo('catalogue');
}

function viewMyOrdersAfterConfirm() {
  const m = document.getElementById('modal-confirmation');
  m.classList.add('hidden');
  m.classList.remove('flex');
  navigateTo('orders');
}

// ── Order History & Buy In Again (Slides 3, 6, 12) ──
async function loadOrdersHistory() {
  const container = document.getElementById('orders-container');
  const emptyMsg = document.getElementById('orders-empty-msg');
  try {
    const orders = await api('/me/orders');
    if (!orders || orders.length === 0) {
      container.innerHTML = '';
      emptyMsg.classList.remove('hidden');
      return;
    }
    emptyMsg.classList.add('hidden');
    container.innerHTML = '';

    orders.forEach(o => {
      const el = document.createElement('div');
      el.className = 'bg-white border border-[#E8E2DC] shadow-xs space-y-4';

      let statusBadge = 'bg-[#FFF0FC] text-[#74318A] border-[#FF9CE9]';
      if (o.status === 'PAID') statusBadge = 'bg-emerald-50 text-emerald-800 border-emerald-300';
      else if (o.status === 'PAYMENT_FAILED') statusBadge = 'bg-rose-50 text-rose-800 border-rose-300';
      else if (o.status === 'CANCELLED') statusBadge = 'bg-[#FAF8F5] text-[#717786] border-[#E8E2DC]';

      const dateStr = new Date(o.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });

      let itemsRows = (o.items || []).map(i => `
        <div class="flex justify-between items-center text-xs py-1.5 border-b border-[#E8E2DC] last:border-b-0">
          <span class="text-[#1E2024]"><b>${escapeHtml(i.title)}</b> &times; ${i.quantity}</span>
          <span class="font-bold text-[#1E2024]">₹${Number(i.lineTotal).toFixed(2)}</span>
        </div>
      `).join('');

      el.innerHTML = `
        <div class="flex items-center justify-between p-4 bg-[#FFEAE6] border-b border-[#FFC2BA]">
          <div>
            <span class="text-sm font-black text-[#1E2024] font-display">Order #${o.id}</span>
            <span class="text-xs text-[#717786] ml-2 font-normal">Placed on ${dateStr}</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="px-2.5 py-0.5 border text-[11px] font-bold uppercase tracking-wider ${statusBadge}">
              ${o.status}
            </span>
            <span class="text-base font-black text-[#1E2024]">₹${Number(o.finalTotal).toFixed(2)}</span>
          </div>
        </div>

        <div class="p-4 space-y-3">
          <div class="bg-[#FAF8F5] p-3 border border-[#E8E2DC] space-y-1">
            ${itemsRows}
          </div>

          <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-2 text-xs">
            <div class="text-[#717786]">
              Delivered to: <b class="text-[#1E2024]">${escapeHtml(o.deliveryAddress.recipient)}</b> (${escapeHtml(o.deliveryAddress.city)}) &bull; Points Used: <b class="text-[#74318A]">${o.pointsRedeemed}</b>
            </div>
            <div class="flex items-center gap-2">
              ${(o.status === 'PENDING_PAYMENT' || o.status === 'PAYMENT_FAILED') ? `
                <button class="px-3 py-1.5 bg-[#FAF8F5] hover:bg-rose-50 text-rose-700 text-xs font-semibold border border-rose-300 transition-colors cursor-pointer" onclick="cancelMyOrder(${o.id})">
                  Cancel Order
                </button>
              ` : ''}
              <button class="px-4 py-1.5 bg-[#74318A] hover:bg-[#5C236E] text-white text-xs font-bold transition-all flex items-center gap-1 shadow-xs cursor-pointer" onclick="buyOrderAgain(${o.id})">
                <span>🔄 Buy In Again</span>
              </button>
            </div>
          </div>
        </div>
      `;
      container.appendChild(el);
    });
  } catch (err) {
    showToast('Failed to load orders: ' + err.message, 'error');
  }
}

async function buyOrderAgain(orderId) {
  try {
    const updatedCart = await api(`/me/orders/${orderId}/buy-again`, { method: 'POST' });
    state.cart = updatedCart;
    updateCartCounter();
    showToast('Re-added order items to your basket!', 'success');
    navigateTo('cart');
  } catch (err) {
    showToast('Buy In Again failed: ' + err.message, 'error');
  }
}

async function cancelMyOrder(orderId) {
  if (!confirm(`Cancel Order #${orderId}? Reserved points and stock will be restored.`)) return;
  try {
    await api(`/me/orders/${orderId}/cancel`, { method: 'POST' });
    showToast(`Order #${orderId} cancelled. Points & stock restored.`, 'info');
    await refreshProfile();
    await loadOrdersHistory();
  } catch (err) {
    showToast('Cancellation failed: ' + err.message, 'error');
  }
}

// ── Utility Helpers ──
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>'"]/g, tag => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    "'": '&#39;',
    '"': '&quot;'
  }[tag] || tag));
}

// ── App Startup ──
window.addEventListener('DOMContentLoaded', async () => {
  await loadCategories();
  await loadPublishers();
  await initAuth();
  await loadBooks();
});
