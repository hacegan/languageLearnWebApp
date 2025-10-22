// Global Variables
let currentLanguage = 'es';
let allWords = [];
let displayedWords = [];
let currentQuizWords = [];
let currentQuizIndex = 0;
let correctCount = 0;
let currentWord = null;
let editingWordId = null;
let isLoading = false;
let hasMoreWords = true;
let currentOffset = 0;
let lastWordId = null;
let viewMode = 'table';
const PAGE_SIZE = 20;

// API Base URL
const API_BASE_URL = window.location.origin + '/api';

// Initialize App
document.addEventListener('DOMContentLoaded', function() {
    console.log('App initializing...');
    console.log('API URL:', API_BASE_URL);

    // Test API connection
    testAPIConnection();

    // Set language from selector
    document.getElementById('languageSelector').addEventListener('change', function(e) {
        currentLanguage = e.target.value;
        resetPagination();
        loadDashboardData();
    });

    // Load initial data
    loadDashboardData();

    // Setup form submission
    document.getElementById('addWordForm').addEventListener('submit', handleAddWord);

    // Setup infinite scroll for all words section
    setupInfiniteScroll();

    // Setup scroll to top button
    window.addEventListener('scroll', function() {
        const scrollToTopBtn = document.getElementById('scrollToTop');
        if (window.pageYOffset > 300) {
            scrollToTopBtn.style.display = 'block';
        } else {
            scrollToTopBtn.style.display = 'none';
        }
    });
});

// Test API Connection
async function testAPIConnection() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/test`);
        const data = await response.json();
        console.log('API Test:', data);
    } catch (error) {
        console.error('API connection error:', error);
        showNotification('API bağlantı hatası!', 'error');
    }
}

// Notification System
function showNotification(message, type = 'info') {
    const toastHtml = `
        <div class="toast align-items-center text-white bg-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'info'} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    const toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
    toastContainer.innerHTML = toastHtml;
    document.body.appendChild(toastContainer);

    const toast = new bootstrap.Toast(toastContainer.querySelector('.toast'));
    toast.show();

    setTimeout(() => toastContainer.remove(), 5000);
}

// Reset Pagination
function resetPagination() {
    currentOffset = 0;
    lastWordId = null;
    hasMoreWords = true;
    allWords = [];
    displayedWords = [];
}

// Setup Infinite Scroll
function setupInfiniteScroll() {
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting && !isLoading && hasMoreWords) {
                const activeSection = document.querySelector('.content-section[style*="block"]');
                if (activeSection && activeSection.id === 'all-words') {
                    loadMoreWords();
                }
            }
        });
    }, {
        rootMargin: '100px'
    });

    const trigger = document.getElementById('scrollTrigger');
    if (trigger) {
        observer.observe(trigger);
    }
}

// Section Navigation
function showSection(sectionId) {
    // Hide all sections
    document.querySelectorAll('.content-section').forEach(section => {
        section.style.display = 'none';
    });

    // Show selected section
    document.getElementById(sectionId).style.display = 'block';

    // Update active nav link
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('onclick')?.includes(sectionId)) {
            link.classList.add('active');
        }
    });

    // Load section specific data
    switch(sectionId) {
        case 'dashboard':
            loadDashboardData();
            break;
        case 'unknown-words':
            loadUnknownWords();
            break;
        case 'new-words':
            loadNewWords();
            break;
        case 'all-words':
            resetPagination();
            loadAllWords();
            break;
    }
}

// Dashboard Functions
async function loadDashboardData() {
    console.log('Loading dashboard for language:', currentLanguage);

    // Show loading spinners
    ['learnedCount', 'learningCount', 'unknownCount', 'totalCount'].forEach(id => {
        document.getElementById(id).innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    });

    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/statistics`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const stats = await response.json();
        console.log('Statistics:', stats);

        // Update stats with animation
        animateCounter('learnedCount', stats.learned || 0);
        animateCounter('learningCount', stats.learning || 0);
        animateCounter('unknownCount', stats.unknown || 0);
        animateCounter('totalCount', stats.total || 0);
    } catch (error) {
        console.error('Error loading dashboard:', error);
        ['learnedCount', 'learningCount', 'unknownCount', 'totalCount'].forEach(id => {
            document.getElementById(id).textContent = '?';
        });
    }
}

// Animate Counter
function animateCounter(elementId, targetValue) {
    const element = document.getElementById(elementId);
    const startValue = 0;
    const duration = 1000;
    const increment = targetValue / (duration / 16);
    let currentValue = startValue;

    const timer = setInterval(() => {
        currentValue += increment;
        if (currentValue >= targetValue) {
            currentValue = targetValue;
            clearInterval(timer);
        }
        element.textContent = Math.floor(currentValue);
    }, 16);
}

// Load Unknown Words with Loading State
async function loadUnknownWords() {
    const container = document.getElementById('unknownWordsList');
    const loadingContainer = document.getElementById('unknownWordsLoading');

    // Show loading
    loadingContainer.style.display = 'flex';
    container.innerHTML = '';

    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/unknown`);
        const words = await response.json();

        // Hide loading
        loadingContainer.style.display = 'none';

        if (words.length === 0) {
            container.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-emoji-smile display-1 text-success"></i>
                    <h3 class="mt-3">Tebrikler!</h3>
                    <p>Bilinmeyen kelime bulunmuyor.</p>
                </div>
            `;
            return;
        }

        words.forEach((word, index) => {
            setTimeout(() => {
                const card = createWordCard(word);
                container.appendChild(card);
                card.style.animation = 'fadeIn 0.5s ease';
            }, index * 50);
        });
    } catch (error) {
        console.error('Error loading unknown words:', error);
        loadingContainer.style.display = 'none';
        showNotification('Kelimeler yüklenirken hata oluştu!', 'error');
    }
}

// Load New Words with Loading State
async function loadNewWords() {
    const container = document.getElementById('newWordsList');
    const loadingContainer = document.getElementById('newWordsLoading');

    // Show loading
    loadingContainer.style.display = 'flex';
    container.innerHTML = '';

    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/new`);
        const words = await response.json();

        // Hide loading
        loadingContainer.style.display = 'none';

        if (words.length === 0) {
            container.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-check-circle display-1 text-info"></i>
                    <h3 class="mt-3">Tüm kelimeler çalışıldı!</h3>
                    <p>Yeni kelime ekleyebilirsiniz.</p>
                </div>
            `;
            return;
        }

        words.forEach((word, index) => {
            setTimeout(() => {
                const card = createWordCard(word);
                container.appendChild(card);
                card.style.animation = 'fadeIn 0.5s ease';
            }, index * 50);
        });
    } catch (error) {
        console.error('Error loading new words:', error);
        loadingContainer.style.display = 'none';
        showNotification('Kelimeler yüklenirken hata oluştu!', 'error');
    }
}

// Load All Words with Lazy Loading
async function loadAllWords() {
    if (isLoading) return;
    isLoading = true;

    // Show appropriate loading indicator
    if (displayedWords.length === 0) {
        document.getElementById('wordsTableBody').innerHTML = `
            <tr>
                <td colspan="7" class="text-center">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Yükleniyor...</span>
                    </div>
                </td>
            </tr>
        `;
    }

    try {
        // Use paginated endpoint
        const response = await fetch(
            `${API_BASE_URL}/words/${currentLanguage}/paginated?limit=${PAGE_SIZE}${lastWordId ? `&lastWordId=${lastWordId}` : ''}`
        );

        const data = await response.json();
        const words = data.words || [];
        hasMoreWords = data.hasMore || false;
        lastWordId = data.lastWordId || null;

        // Add new words to arrays
        allWords.push(...words);
        displayedWords.push(...words);

        // Display words based on view mode
        if (viewMode === 'table') {
            displayWordsTable(displayedWords);
        } else {
            displayWordsCards(displayedWords);
        }

        // Show/hide load more button
        const loadMoreBtn = document.getElementById('loadMoreBtn');
        if (hasMoreWords) {
            loadMoreBtn.style.display = 'inline-block';
        } else {
            loadMoreBtn.style.display = 'none';
        }

        // Update scroll trigger visibility
        const scrollTrigger = document.getElementById('scrollTrigger');
        if (scrollTrigger) {
            scrollTrigger.style.display = hasMoreWords ? 'block' : 'none';
        }

    } catch (error) {
        console.error('Error loading all words:', error);
        showNotification('Kelimeler yüklenirken hata oluştu!', 'error');
    } finally {
        isLoading = false;

        // Hide spinner in load more button
        const spinner = document.getElementById('loadMoreSpinner');
        if (spinner) {
            spinner.classList.add('d-none');
        }
    }
}

// Load More Words
async function loadMoreWords() {
    if (isLoading || !hasMoreWords) return;

    // Show spinner
    const spinner = document.getElementById('loadMoreSpinner');
    const scrollSpinner = document.querySelector('.infinite-scroll-trigger .loading-spinner');

    if (spinner) spinner.classList.remove('d-none');
    if (scrollSpinner) scrollSpinner.classList.remove('d-none');

    await loadAllWords();

    if (scrollSpinner) scrollSpinner.classList.add('d-none');
}

// Create Word Card with Lazy Loading
function createWordCard(word) {
    const col = document.createElement('div');
    col.className = 'col-md-4 col-sm-6 mb-4';

    const difficultyBadge = getDifficultyBadge(word.difficulty);
    const categoryBadge = getCategoryBadge(word.category);

    col.innerHTML = `
        <div class="word-card card h-100">
            <div class="card-header d-flex justify-content-between align-items-center">
                <strong>${word.word}</strong>
                <button class="btn btn-sm btn-light btn-audio" 
                        onclick="event.stopPropagation(); playWordAudio('${word.word}')">
                    <i class="bi bi-volume-up"></i>
                </button>
            </div>
            ${word.imageUrl ? `
                <img src="${word.imageUrl}" 
                     class="card-img-top" 
                     style="height: 150px; object-fit: cover;"
                     loading="lazy"
                     alt="${word.word}"
                     onerror="this.onerror=null; this.src='https://via.placeholder.com/400x300?text=${encodeURIComponent(word.word)}'; this.style.opacity='0.5';"
                     onload="this.classList.add('loaded')">
            ` : ''}
            <div class="card-body">
                <h5 class="card-title">${word.translation}</h5>
                <div class="mb-2">
                    ${categoryBadge} ${difficultyBadge}
                </div>
                ${word.example ? `<p class="card-text small text-muted"><em>${word.example}</em></p>` : ''}
                <div class="d-flex justify-content-between align-items-center mt-3">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-success" onclick="updateWordProgress('${word.id}', true)" title="Doğru">
                            <i class="bi bi-check"></i> ${word.correctCount || 0}
                        </button>
                        <button class="btn btn-outline-danger" onclick="updateWordProgress('${word.id}', false)" title="Yanlış">
                            <i class="bi bi-x"></i> ${word.incorrectCount || 0}
                        </button>
                    </div>
                    <button class="btn btn-sm ${word.isFavorite ? 'btn-warning' : 'btn-outline-warning'}" 
                            onclick="toggleFavorite('${word.id}')" title="Favori">
                        <i class="bi ${word.isFavorite ? 'bi-star-fill' : 'bi-star'}"></i>
                    </button>
                </div>
            </div>
        </div>
    `;

    return col;
}

// Display Words in Table
function displayWordsTable(words) {
    const tbody = document.getElementById('wordsTableBody');

    if (displayedWords.length === 0) {
        tbody.innerHTML = '';
    } else if (words === displayedWords) {
        // Full refresh
        tbody.innerHTML = '';
    }

    // Only add new words
    const startIndex = tbody.children.length;
    const newWords = words.slice(startIndex);

    newWords.forEach(word => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <strong>${word.word}</strong>
                <button class="btn btn-sm btn-link" onclick="playWordAudio('${word.word}')">
                    <i class="bi bi-volume-up"></i>
                </button>
            </td>
            <td>${word.translation}</td>
            <td>${getCategoryBadge(word.category)}</td>
            <td>${getDifficultyBadge(word.difficulty)}</td>
            <td><span class="text-success">${word.correctCount || 0}</span></td>
            <td><span class="text-danger">${word.incorrectCount || 0}</span></td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="practiceWord('${word.id}')" title="Pratik">
                    <i class="bi bi-play"></i>
                </button>
                <button class="btn btn-sm btn-warning" onclick="editWord('${word.id}')" title="Düzenle">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm ${word.isFavorite ? 'btn-warning' : 'btn-outline-warning'}" 
                        onclick="toggleFavorite('${word.id}')" title="Favori">
                    <i class="bi ${word.isFavorite ? 'bi-star-fill' : 'bi-star'}"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="deleteWord('${word.id}')" title="Sil">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
}

// Display Words in Card View
function displayWordsCards(words) {
    const container = document.getElementById('cardView');

    if (displayedWords.length === 0) {
        container.innerHTML = '';
    } else if (words === displayedWords) {
        container.innerHTML = '';
    }

    const startIndex = container.children.length;
    const newWords = words.slice(startIndex);

    newWords.forEach((word, index) => {
        setTimeout(() => {
            const card = createWordCard(word);
            container.appendChild(card);
        }, index * 30);
    });
}

// Set View Mode
function setViewMode(mode) {
    viewMode = mode;

    // Update button states
    document.getElementById('tableViewBtn').classList.toggle('active', mode === 'table');
    document.getElementById('cardViewBtn').classList.toggle('active', mode === 'card');

    // Show/hide views
    document.getElementById('tableView').style.display = mode === 'table' ? 'block' : 'none';
    document.getElementById('cardView').style.display = mode === 'card' ? 'block' : 'none';

    // Refresh display
    if (mode === 'table') {
        displayWordsTable(displayedWords);
    } else {
        displayWordsCards(displayedWords);
    }
}

// Filter Words
function filterWords() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const category = document.getElementById('categoryFilter').value;
    const difficulty = document.getElementById('difficultyFilter').value;

    const filtered = allWords.filter(word => {
        const matchesSearch = word.word.toLowerCase().includes(searchTerm) ||
            word.translation.toLowerCase().includes(searchTerm);
        const matchesCategory = !category || word.category === category;
        const matchesDifficulty = !difficulty || word.difficulty === difficulty;

        return matchesSearch && matchesCategory && matchesDifficulty;
    });

    displayedWords = filtered;

    if (viewMode === 'table') {
        displayWordsTable(filtered);
    } else {
        displayWordsCards(filtered);
    }
}

// Debounce function
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Apply debounce to search
document.getElementById('searchInput').addEventListener('keyup', debounce(filterWords, 300));

// Get Difficulty Badge
function getDifficultyBadge(difficulty) {
    const badges = {
        'easy': '<span class="badge bg-success">Kolay</span>',
        'medium': '<span class="badge bg-warning">Orta</span>',
        'hard': '<span class="badge bg-danger">Zor</span>'
    };
    return badges[difficulty] || '<span class="badge bg-secondary">Belirsiz</span>';
}

// Get Category Badge
function getCategoryBadge(category) {
    const badges = {
        'verb': '<span class="badge bg-info">Fiil</span>',
        'noun': '<span class="badge bg-primary">İsim</span>',
        'adjective': '<span class="badge bg-purple">Sıfat</span>',
        'adverb': '<span class="badge bg-secondary">Zarf</span>'
    };
    return badges[category] || '<span class="badge bg-dark">Diğer</span>';
}

// Quiz Functions
async function startQuiz() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/quiz?count=10`);
        currentQuizWords = await response.json();

        if (currentQuizWords.length === 0) {
            showNotification('Quiz başlatmak için yeterli kelime yok!', 'error');
            return;
        }

        currentQuizIndex = 0;
        correctCount = 0;

        // Hide start screen, show quiz content
        document.getElementById('quizStart').style.display = 'none';
        document.getElementById('quizResult').style.display = 'none';
        document.getElementById('quizContent').style.display = 'block';

        // Update counters
        document.getElementById('correctAnswers').textContent = '0';
        document.getElementById('questionNumber').textContent = '1';

        loadQuizQuestion();
    } catch (error) {
        console.error('Error starting quiz:', error);
        showNotification('Quiz başlatılırken hata oluştu!', 'error');
    }
}

// Load Quiz Question
function loadQuizQuestion() {
    if (currentQuizIndex >= currentQuizWords.length) {
        showQuizResult();
        return;
    }

    currentWord = currentQuizWords[currentQuizIndex];

    // Display word with image if available
    const quizWord = document.getElementById('quizWord');
    quizWord.innerHTML = `
        ${currentWord.word}
        ${currentWord.imageUrl ? `
            <div class="mt-3">
                <img src="${currentWord.imageUrl}" 
                     class="img-fluid rounded" 
                     style="max-height: 200px;"
                     loading="lazy"
                     onerror="this.style.display='none'">
            </div>
        ` : ''}
    `;

    // Create answer options
    const options = generateAnswerOptions(currentWord);
    const optionsContainer = document.getElementById('answerOptions');
    optionsContainer.innerHTML = '';

    options.forEach((option, index) => {
        const col = document.createElement('div');
        col.className = 'col-md-6';
        const optionId = `option-${index}`;
        col.innerHTML = `
            <div class="answer-option card p-3 mb-2" 
                 id="${optionId}" 
                 data-answer="${option.replace(/"/g, '&quot;')}"
                 style="cursor: pointer; transition: all 0.3s;">
                <i class="bi bi-circle me-2"></i>
                ${option}
            </div>
        `;
        optionsContainer.appendChild(col);
    });

    // Add event listeners
    setTimeout(() => {
        document.querySelectorAll('.answer-option').forEach(opt => {
            opt.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                const answer = this.getAttribute('data-answer');
                checkAnswer(answer, this);
            });
        });
    }, 100);

    // Reset UI
    document.getElementById('feedback').style.display = 'none';
    document.getElementById('nextQuestionBtn').style.display = 'none';

    // Auto play audio
    setTimeout(() => {
        playWordAudio(currentWord.word);
    }, 500);
}

// Generate Answer Options
function generateAnswerOptions(word) {
    const correctAnswer = word.translation;
    const options = [correctAnswer];

    // Add random wrong answers
    const allTranslations = currentQuizWords
        .map(w => w.translation)
        .filter(t => t !== correctAnswer);

    while (options.length < 4 && allTranslations.length > 0) {
        const randomIndex = Math.floor(Math.random() * allTranslations.length);
        const wrongAnswer = allTranslations.splice(randomIndex, 1)[0];
        if (!options.includes(wrongAnswer)) {
            options.push(wrongAnswer);
        }
    }

    // If not enough options, add dummy ones
    while (options.length < 4) {
        options.push(`Opción ${options.length + 1}`);
    }

    // Shuffle options
    return options.sort(() => Math.random() - 0.5);
}

// Check Answer
async function checkAnswer(answer, element) {
    // Prevent double click
    if (!element || element.classList.contains('disabled')) {
        return;
    }

    // Disable all options
    document.querySelectorAll('.answer-option').forEach(opt => {
        opt.classList.add('disabled');
        opt.style.pointerEvents = 'none';
    });

    const isCorrect = answer === currentWord.translation;

    // Update UI
    if (isCorrect) {
        element.classList.add('border-success', 'bg-success', 'text-white');
        element.querySelector('i').className = 'bi bi-check-circle-fill me-2';
        correctCount++;
        document.getElementById('correctAnswers').textContent = correctCount;
        showFeedback(true, 'Doğru! 🎉');
        playSuccessSound();
    } else {
        element.classList.add('border-danger', 'bg-danger', 'text-white');
        element.querySelector('i').className = 'bi bi-x-circle-fill me-2';
        showFeedback(false, `Yanlış! Doğru cevap: ${currentWord.translation}`);
        playErrorSound();

        // Highlight correct answer
        document.querySelectorAll('.answer-option').forEach(opt => {
            const optAnswer = opt.getAttribute('data-answer');
            if (optAnswer === currentWord.translation) {
                opt.classList.add('border-success', 'bg-success', 'text-white');
                opt.querySelector('i').className = 'bi bi-check-circle-fill me-2';
            }
        });
    }

    // Update word progress
    try {
        await fetch(`${API_BASE_URL}/words/${currentLanguage}/${currentWord.id}/progress`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correct: isCorrect })
        });
    } catch (error) {
        console.error('Error updating progress:', error);
    }

    // Show next button
    document.getElementById('nextQuestionBtn').style.display = 'inline-block';
}

// Show Feedback
function showFeedback(isCorrect, message) {
    const feedback = document.getElementById('feedback');
    feedback.className = `alert alert-${isCorrect ? 'success' : 'danger'}`;
    feedback.textContent = message;
    feedback.style.display = 'block';
}

// Next Question
function nextQuestion() {
    currentQuizIndex++;
    document.getElementById('questionNumber').textContent = currentQuizIndex + 1;
    loadQuizQuestion();
}

// Show Quiz Result
function showQuizResult() {
    document.getElementById('quizContent').style.display = 'none';
    document.getElementById('quizResult').style.display = 'block';

    const percentage = (correctCount / currentQuizWords.length) * 100;
    document.getElementById('finalScore').textContent = correctCount;

    let message = '';
    if (percentage >= 80) {
        message = 'Mükemmel! 🏆';
    } else if (percentage >= 60) {
        message = 'İyi iş! 👍';
    } else if (percentage >= 40) {
        message = 'Fena değil, pratik yapmaya devam! 📚';
    } else {
        message = 'Daha fazla çalışmaya ihtiyacın var! 💪';
    }

    document.getElementById('resultMessage').textContent = message;
}

// Audio Functions
function playWordAudio(word) {
    if (!word) return;

    // Use Web Speech API
    if ('speechSynthesis' in window) {
        speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(word);
        utterance.lang = currentLanguage === 'es' ? 'es-ES' : 'en-US';
        utterance.rate = 0.9;
        utterance.pitch = 1;
        utterance.volume = 0.8;
        speechSynthesis.speak(utterance);
    }
}

function playAudio() {
    if (currentWord) {
        playWordAudio(currentWord.word);
    }
}

function playSuccessSound() {
    const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBSuBzvLZiTYIG2m98OScTgwOUort9bllHgU7k9n1zn0wBSh+zPTaizsIHWq+8uifUAoMT6rs9b1pHgg2k9jzzXkxBSh+zPLaizsIHWu+8+mjVQoLTKns87xmHgg3k9jzzXkxBSh+zPLaizsIHWu+8+mjVQoLTKns87xmHgg3k9jzzXkxBCh+zPLaizsIHWu+8+mjVQoLTKns87xmHgg3k9jzzXkxBCh+zPLaizsIHWu+8+mjVQoLTKns87xmHgg3k9jzzXkxBCh+zPLaizsIHWu+8+mjVQoLTKns87xmHgg3k9jzzXkxBCh+zPLaizsIHWu+8+mjVQoLTKns87xmHgg3k9jzzXkxBCh+zPLaizsi');
    audio.volume = 0.3;
    audio.play().catch(() => {});
}

function playErrorSound() {
    const audio = new Audio('data:audio/wav;base64,UklGRuYCAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YcICAAC4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4uLi4');
    audio.volume = 0.3;
    audio.play().catch(() => {});
}

// CRUD Operations
async function handleAddWord(e) {
    e.preventDefault();

    const word = {
        word: document.getElementById('wordInput').value,
        translation: document.getElementById('translationInput').value,
        category: document.getElementById('categoryInput').value,
        difficulty: document.getElementById('difficultyInput').value,
        example: document.getElementById('exampleInput').value,
        pronunciation: document.getElementById('pronunciationInput').value,
        tags: document.getElementById('tagsInput').value.split(',').map(t => t.trim()).filter(t => t)
    };

    try {
        const url = editingWordId
            ? `${API_BASE_URL}/words/${currentLanguage}/${editingWordId}`
            : `${API_BASE_URL}/words/${currentLanguage}`;

        const method = editingWordId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(word)
        });

        if (response.ok) {
            showNotification(editingWordId ? 'Kelime güncellendi!' : 'Kelime eklendi!', 'success');
            document.getElementById('addWordForm').reset();
            editingWordId = null;
            showSection('all-words');
        } else {
            showNotification('İşlem sırasında hata oluştu!', 'error');
        }
    } catch (error) {
        console.error('Error:', error);
        showNotification('İşlem sırasında hata oluştu!', 'error');
    }
}

function editWord(wordId) {
    const word = allWords.find(w => w.id === wordId);
    if (word) {
        editingWordId = wordId;

        document.getElementById('wordInput').value = word.word;
        document.getElementById('translationInput').value = word.translation;
        document.getElementById('categoryInput').value = word.category || 'other';
        document.getElementById('difficultyInput').value = word.difficulty || 'medium';
        document.getElementById('exampleInput').value = word.example || '';
        document.getElementById('pronunciationInput').value = word.pronunciation || '';
        document.getElementById('tagsInput').value = word.tags ? word.tags.join(', ') : '';

        showSection('add-word');
    }
}

async function deleteWord(wordId) {
    if (confirm('Bu kelimeyi silmek istediğinize emin misiniz?')) {
        try {
            const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/${wordId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                showNotification('Kelime silindi!', 'success');
                // Remove from arrays
                allWords = allWords.filter(w => w.id !== wordId);
                displayedWords = displayedWords.filter(w => w.id !== wordId);
                // Refresh display
                if (viewMode === 'table') {
                    displayWordsTable(displayedWords);
                } else {
                    displayWordsCards(displayedWords);
                }
            } else {
                showNotification('Silme işlemi başarısız!', 'error');
            }
        } catch (error) {
            console.error('Error deleting word:', error);
            showNotification('Silme işlemi başarısız!', 'error');
        }
    }
}

async function toggleFavorite(wordId) {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/${wordId}/favorite`, {
            method: 'PUT'
        });

        if (response.ok) {
            const updatedWord = await response.json();

            // Update in arrays
            const index = allWords.findIndex(w => w.id === wordId);
            if (index !== -1) {
                allWords[index].isFavorite = updatedWord.isFavorite;
                displayedWords = [...allWords];
            }

            // Refresh current view
            const activeSection = document.querySelector('.content-section[style*="block"]');
            if (activeSection) {
                const sectionId = activeSection.id;
                if (sectionId === 'all-words') {
                    if (viewMode === 'table') {
                        displayWordsTable(displayedWords);
                    } else {
                        displayWordsCards(displayedWords);
                    }
                } else if (sectionId === 'unknown-words') {
                    loadUnknownWords();
                } else if (sectionId === 'new-words') {
                    loadNewWords();
                }
            }
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
        showNotification('Favori durumu güncellenemedi!', 'error');
    }
}

async function updateWordProgress(wordId, isCorrect) {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/${wordId}/progress`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ correct: isCorrect })
        });

        if (response.ok) {
            const updatedWord = await response.json();

            // Update in arrays
            const index = allWords.findIndex(w => w.id === wordId);
            if (index !== -1) {
                allWords[index] = updatedWord;
                displayedWords = [...allWords];
            }

            showNotification(isCorrect ? 'Doğru işaretlendi!' : 'Yanlış işaretlendi!', 'info');

            // Refresh display
            if (viewMode === 'table') {
                displayWordsTable(displayedWords);
            } else {
                displayWordsCards(displayedWords);
            }
        }
    } catch (error) {
        console.error('Error updating progress:', error);
        showNotification('İlerleme güncellenemedi!', 'error');
    }
}

function practiceWord(wordId) {
    const word = allWords.find(w => w.id === wordId);
    if (word) {
        currentQuizWords = [word];

        // Add random words for options
        const otherWords = allWords.filter(w => w.id !== wordId);
        const randomWords = otherWords.sort(() => 0.5 - Math.random()).slice(0, 3);
        currentQuizWords.push(...randomWords);

        currentQuizIndex = 0;
        correctCount = 0;

        showSection('quiz');
        document.getElementById('quizStart').style.display = 'none';
        document.getElementById('quizResult').style.display = 'none';
        document.getElementById('quizContent').style.display = 'block';

        document.getElementById('correctAnswers').textContent = '0';
        document.getElementById('questionNumber').textContent = '1';

        loadQuizQuestion();
    }
}

function startQuickQuiz() {
    showSection('quiz');
    startQuiz();
}

function reviewWords() {
    showSection('unknown-words');
}

function scrollToTop() {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}