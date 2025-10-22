// Global Variables
let currentLanguage = 'es';
let allWords = [];
let currentQuizWords = [];
let currentQuizIndex = 0;
let correctCount = 0;
let currentWord = null;
let editingWordId = null;

// API Base URL - window.location kullan
const API_BASE_URL = window.location.origin + '/api';

// Initialize App
document.addEventListener('DOMContentLoaded', function() {
    console.log('App initializing...');
    console.log('API URL:', API_BASE_URL);

    // Test API connection
    fetch(`${API_BASE_URL}/words/test`)
        .then(response => response.json())
        .then(data => {
            console.log('API Test:', data);
        })
        .catch(error => {
            console.error('API connection error:', error);
        });

    // Set language from selector
    document.getElementById('languageSelector').addEventListener('change', function(e) {
        currentLanguage = e.target.value;
        loadDashboardData();
    });

    // Load initial data
    loadDashboardData();

    // Setup form submission
    document.getElementById('addWordForm').addEventListener('submit', handleAddWord);
});

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
            loadAllWords();
            break;
    }
}

// Dashboard Functions
async function loadDashboardData() {
    console.log('Loading dashboard for language:', currentLanguage);
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/statistics`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const stats = await response.json();
        console.log('Statistics:', stats);

        // Update stats
        document.getElementById('learnedCount').textContent = stats.learned || 0;
        document.getElementById('learningCount').textContent = stats.learning || 0;
        document.getElementById('unknownCount').textContent = stats.unknown || 0;
        document.getElementById('totalCount').textContent = stats.total || 0;
    } catch (error) {
        console.error('Error loading dashboard:', error);
        // Try alternative method
        loadDashboardDataAlternative();
    }
}

async function loadDashboardDataAlternative() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}`);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const words = await response.json();

        // Calculate statistics
        const learned = words.filter(w => (w.correctCount || 0) >= 5).length;
        const learning = words.filter(w => (w.correctCount || 0) > 0 && (w.correctCount || 0) < 5).length;
        const unknown = words.filter(w => (w.correctCount || 0) === 0).length;

        // Update stats
        document.getElementById('learnedCount').textContent = learned;
        document.getElementById('learningCount').textContent = learning;
        document.getElementById('unknownCount').textContent = unknown;
        document.getElementById('totalCount').textContent = words.length;
    } catch (error) {
        console.error('Error loading dashboard alternative:', error);
        // Show error message
        document.getElementById('learnedCount').textContent = '?';
        document.getElementById('learningCount').textContent = '?';
        document.getElementById('unknownCount').textContent = '?';
        document.getElementById('totalCount').textContent = '?';
    }
}

// Quiz Functions
async function startQuiz() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/quiz?count=10`);
        currentQuizWords = await response.json();

        if (currentQuizWords.length === 0) {
            alert('Quiz başlatmak için yeterli kelime yok!');
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
        alert('Quiz başlatılırken hata oluştu!');
    }
}

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
        ${currentWord.imageUrl ? `<img src="${currentWord.imageUrl}" class="img-fluid mt-3 rounded" style="max-height: 200px;">` : ''}
    `;

    // Create answer options
    const options = generateAnswerOptions(currentWord);
    const optionsContainer = document.getElementById('answerOptions');
    optionsContainer.innerHTML = '';

    options.forEach(option => {
        const col = document.createElement('div');
        col.className = 'col-md-6';
        col.innerHTML = `
            <div class="answer-option" onclick="checkAnswer('${option.replace(/'/g, "\\'")}', this)">
                <i class="bi bi-circle me-2"></i>
                ${option}
            </div>
        `;
        optionsContainer.appendChild(col);
    });

    // Reset UI
    document.getElementById('feedback').style.display = 'none';
    document.getElementById('nextQuestionBtn').style.display = 'none';
}

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

async function checkAnswer(answer, element) {
    // Prevent multiple clicks
    if (element.classList.contains('correct') || element.classList.contains('incorrect')) {
        return;
    }

    const isCorrect = answer === currentWord.translation;

    // Update UI
    if (isCorrect) {
        element.classList.add('correct');
        element.querySelector('i').className = 'bi bi-check-circle-fill me-2 text-success';
        correctCount++;
        document.getElementById('correctAnswers').textContent = correctCount;
        showFeedback(true, 'Doğru! 🎉');
    } else {
        element.classList.add('incorrect');
        element.querySelector('i').className = 'bi bi-x-circle-fill me-2 text-danger';
        showFeedback(false, `Yanlış! Doğru cevap: ${currentWord.translation}`);

        // Highlight correct answer
        document.querySelectorAll('.answer-option').forEach(opt => {
            if (opt.textContent.trim().includes(currentWord.translation)) {
                opt.classList.add('correct');
                opt.querySelector('i').className = 'bi bi-check-circle-fill me-2 text-success';
            }
        });
    }

    // Disable all options
    document.querySelectorAll('.answer-option').forEach(opt => {
        opt.style.pointerEvents = 'none';
    });

    // Update word progress in backend
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

function showFeedback(isCorrect, message) {
    const feedback = document.getElementById('feedback');
    feedback.className = `alert alert-${isCorrect ? 'success' : 'danger'}`;
    feedback.textContent = message;
    feedback.style.display = 'block';
}

function nextQuestion() {
    currentQuizIndex++;
    document.getElementById('questionNumber').textContent = currentQuizIndex + 1;
    loadQuizQuestion();
}

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

function startQuickQuiz() {
    showSection('quiz');
    startQuiz();
}

// Word List Functions
async function loadUnknownWords() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/unknown`);
        const words = await response.json();

        const container = document.getElementById('unknownWordsList');
        container.innerHTML = '';

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

        words.forEach(word => {
            const card = createWordCard(word);
            container.appendChild(card);
        });
    } catch (error) {
        console.error('Error loading unknown words:', error);
    }
}

async function loadNewWords() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/new`);
        const words = await response.json();

        const container = document.getElementById('newWordsList');
        container.innerHTML = '';

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

        words.forEach(word => {
            const card = createWordCard(word);
            container.appendChild(card);
        });
    } catch (error) {
        console.error('Error loading new words:', error);
    }
}

async function loadAllWords() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}`);
        allWords = await response.json();
        displayWordsTable(allWords);
    } catch (error) {
        console.error('Error loading all words:', error);
    }
}

function createWordCard(word) {
    const col = document.createElement('div');
    col.className = 'col-md-4 col-sm-6 mb-4';

    const difficultyBadge = getDifficultyBadge(word.difficulty);
    const categoryBadge = getCategoryBadge(word.category);

    col.innerHTML = `
        <div class="word-card">
            <div class="card-header">
                ${word.word}
                <button class="btn btn-sm btn-light float-end btn-audio" onclick="playWordAudio('${word.word}')">
                    <i class="bi bi-volume-up"></i>
                </button>
            </div>
            ${word.imageUrl ? `<img src="${word.imageUrl}" class="card-img-top" style="height: 150px; object-fit: cover;">` : ''}
            <div class="card-body">
                <h6 class="card-subtitle mb-2 text-muted">${word.translation}</h6>
                ${word.pronunciation ? `<small class="text-muted d-block mb-2">[${word.pronunciation}]</small>` : ''}
                ${word.example ? `<p class="small mb-2"><em>"${word.example}"</em></p>` : ''}
                
                <div class="d-flex justify-content-between align-items-center mt-3">
                    <div>
                        ${difficultyBadge}
                        ${categoryBadge}
                    </div>
                    <div>
                        <button class="btn btn-sm ${word.isFavorite ? 'btn-warning' : 'btn-outline-warning'}" 
                                onclick="toggleFavorite('${word.id}')" title="Favori">
                            <i class="bi ${word.isFavorite ? 'bi-star-fill' : 'bi-star'}"></i>
                        </button>
                    </div>
                </div>
                
                <div class="mt-2">
                    <span class="badge bg-success" title="Doğru sayısı">
                        <i class="bi bi-check"></i> ${word.correctCount || 0}
                    </span>
                    <span class="badge bg-danger" title="Yanlış sayısı">
                        <i class="bi bi-x"></i> ${word.incorrectCount || 0}
                    </span>
                    <span class="badge bg-info" title="Çalışma sayısı">
                        <i class="bi bi-book"></i> ${word.studyCount || 0}
                    </span>
                </div>
                
                <div class="mt-3">
                    <button class="btn btn-sm btn-primary w-100" onclick="practiceWord('${word.id}')">
                        <i class="bi bi-play-circle"></i> Pratik Yap
                    </button>
                </div>
            </div>
        </div>
    `;

    return col;
}

function getDifficultyBadge(difficulty) {
    const badges = {
        'easy': '<span class="badge bg-success">Kolay</span>',
        'medium': '<span class="badge bg-warning">Orta</span>',
        'hard': '<span class="badge bg-danger">Zor</span>'
    };
    return badges[difficulty] || '<span class="badge bg-secondary">Belirsiz</span>';
}

function getCategoryBadge(category) {
    const badges = {
        'verb': '<span class="badge bg-info">Fiil</span>',
        'noun': '<span class="badge bg-primary">İsim</span>',
        'adjective': '<span class="badge bg-purple">Sıfat</span>',
        'adverb': '<span class="badge bg-secondary">Zarf</span>'
    };
    return badges[category] || '<span class="badge bg-dark">Diğer</span>';
}

function displayWordsTable(words) {
    const tbody = document.getElementById('wordsTableBody');
    tbody.innerHTML = '';

    words.forEach(word => {
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

// Filter Functions
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

    displayWordsTable(filtered);
}

// Add Word Function
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
            alert(editingWordId ? 'Kelime güncellendi!' : 'Kelime eklendi!');
            document.getElementById('addWordForm').reset();
            editingWordId = null;
            showSection('all-words');
        } else {
            alert('İşlem sırasında hata oluştu!');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('İşlem sırasında hata oluştu!');
    }
}

// Edit Word Function
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

// Delete Word Function
async function deleteWord(wordId) {
    if (confirm('Bu kelimeyi silmek istediğinize emin misiniz?')) {
        try {
            const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/${wordId}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                alert('Kelime silindi!');
                loadAllWords();
            } else {
                alert('Silme işlemi başarısız!');
            }
        } catch (error) {
            console.error('Error deleting word:', error);
            alert('Silme işlemi başarısız!');
        }
    }
}

// Toggle Favorite Function
async function toggleFavorite(wordId) {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/${wordId}/favorite`, {
            method: 'PUT'
        });

        if (response.ok) {
            // Reload current view
            const activeSection = document.querySelector('.content-section[style*="block"]');
            if (activeSection) {
                const sectionId = activeSection.id;
                if (sectionId === 'all-words') {
                    loadAllWords();
                } else if (sectionId === 'unknown-words') {
                    loadUnknownWords();
                } else if (sectionId === 'new-words') {
                    loadNewWords();
                }
            }
        }
    } catch (error) {
        console.error('Error toggling favorite:', error);
    }
}

// Audio Functions - Google Translate TTS kullan
function playWordAudio(word) {
    // Google Translate TTS API
    const lang = currentLanguage === 'es' ? 'es' : 'en';
    const audioUrl = `https://translate.google.com/translate_tts?ie=UTF-8&tl=${lang}&client=tw-ob&q=${encodeURIComponent(word)}`;

    const audio = new Audio(audioUrl);
    audio.play().catch(error => {
        // Fallback to Web Speech API
        if ('speechSynthesis' in window) {
            const utterance = new SpeechSynthesisUtterance(word);
            utterance.lang = currentLanguage === 'es' ? 'es-ES' : 'en-US';
            utterance.rate = 0.8;
            speechSynthesis.speak(utterance);
        } else {
            console.error('Audio playback failed:', error);
        }
    });
}

function playAudio() {
    if (currentWord) {
        playWordAudio(currentWord.word);
    }
}

// Practice Function
function practiceWord(wordId) {
    const word = allWords.find(w => w.id === wordId);
    if (word) {
        currentQuizWords = [word];
        // Add some random words for options
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

// Review Function
function reviewWords() {
    showSection('unknown-words');
}

// Migrate existing words on first load
async function migrateWords() {
    try {
        const response = await fetch(`${API_BASE_URL}/words/${currentLanguage}/migrate`, {
            method: 'POST'
        });

        if (response.ok) {
            console.log('Migration completed successfully');
        }
    } catch (error) {
        console.error('Migration error:', error);
    }
}

// Call migration on first load
setTimeout(migrateWords, 1000);