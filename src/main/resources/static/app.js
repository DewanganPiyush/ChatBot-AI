// Professional Health Catalyst Chatbot - Enhanced JavaScript

let sessionId = null;
let isLoading = false;
let messageCount = 0;
let healthCatalystKnowledge = null;

document.addEventListener('DOMContentLoaded', function() {
    // Load embedded knowledge base
    loadHealthCatalystKnowledge();

    // Generate session ID
    sessionId = generateSessionId();

    // Initialize chat
    initializeChat();

    // Setup form submission
    const form = document.getElementById('chat-form');
    form.addEventListener('submit', handleFormSubmit);

    // Setup input character counter
    const input = document.getElementById('message-input');
    const charCount = document.querySelector('.char-count');

    input.addEventListener('input', function() {
        const count = input.value.length;
        charCount.textContent = `${count}/1000`;

        // Change color based on character count
        if (count > 800) {
            charCount.style.color = '#dc2626';
        } else if (count > 600) {
            charCount.style.color = '#f59e0b';
        } else {
            charCount.style.color = '#6b7280';
        }
    });

    // Setup Enter key submission
    input.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleFormSubmit(e);
        }
    });

    // Auto-focus input
    input.focus();
});

function loadHealthCatalystKnowledge() {
    try {
        const knowledgeScript = document.getElementById('health-catalyst-knowledge');
        if (knowledgeScript) {
            healthCatalystKnowledge = JSON.parse(knowledgeScript.textContent);
            console.log('Health Catalyst knowledge base loaded successfully');
        }
    } catch (error) {
        console.error('Error loading knowledge base:', error);
    }
}

function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

function initializeChat() {
    const chatBox = document.getElementById('chat-box');
    chatBox.innerHTML = ''; // Clear any existing content
}

async function handleFormSubmit(e) {
    e.preventDefault();

    if (isLoading) return;

    const input = document.getElementById('message-input');
    const message = input.value.trim();

    if (!message) return;

    // Clear input and add user message
    input.value = '';
    document.querySelector('.char-count').textContent = '0/1000';
    document.querySelector('.char-count').style.color = '#6b7280';

    // Add user message to chat
    addMessage(message, 'user');

    // Hide welcome section after first message
    hideWelcomeSection();

    // Show professional typing indicator with bouncing dots
    showProfessionalTypingIndicator();
    isLoading = true;

    try {
        // Prepare enhanced request with knowledge base context
        const enhancedRequest = {
            message: message,
            sessionId: sessionId,
            knowledgeBase: healthCatalystKnowledge,
            context: generateContextPrompt(message)
        };

        const response = await fetch('/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(enhancedRequest)
        });

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const data = await response.json();

        // Update session ID if provided
        if (data.sessionId) {
            sessionId = data.sessionId;
        }

        // Hide typing indicator and add bot response
        hideProfessionalTypingIndicator();
        addMessage(data.response || 'Sorry, I could not process your request.', 'bot');

    } catch (error) {
        console.error('Error:', error);
        hideProfessionalTypingIndicator();
        addMessage('Sorry, I\'m experiencing technical difficulties. Please try again or contact IT support at hcatindia.itops@healthcatalyst.com.', 'bot');
    } finally {
        isLoading = false;
        input.focus();
    }
}

function generateContextPrompt(userMessage) {
    if (!healthCatalystKnowledge) return '';

    // Create a comprehensive context for the AI
    return `You are the official Health Catalyst India HR Assistant chatbot. Use the provided knowledge base to answer questions accurately and professionally.

    Company: ${healthCatalystKnowledge.companyInfo.name}
    Mission: ${healthCatalystKnowledge.companyInfo.mission}

    Key Guidelines:
    - Always provide accurate information based on the knowledge base
    - For leave applications, direct users to Workday
    - For technical issues, refer to IT support: ${healthCatalystKnowledge.contacts.itSupport}
    - For HR queries, refer to: ${healthCatalystKnowledge.contacts.hr}
    - Be helpful, professional, and concise
    - If you don't know something, admit it and suggest contacting the appropriate department

    User Question: ${userMessage}`;
}

function addMessage(message, sender) {
    const chatBox = document.getElementById('chat-box');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;
    messageCount++;

    const timestamp = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    const icon = sender === 'user' ? 'fas fa-user' : 'fas fa-robot';
    const senderName = sender === 'user' ? 'You' : 'Health Catalyst Chatbot';

    // Format the message content for better display
    let formattedMessage = formatMessageContent(message);

    messageDiv.innerHTML = `
        <div class="message-content">${formattedMessage}</div>
        <div class="message-info">
            <i class="${icon}"></i>
            <span>${senderName}</span>
            <span>•</span>
            <span>${timestamp}</span>
        </div>
    `;

    chatBox.appendChild(messageDiv);

    // Smooth scroll to bottom
    setTimeout(() => {
        chatBox.scrollTo({
            top: chatBox.scrollHeight,
            behavior: 'smooth'
        });
    }, 100);
}

function formatMessageContent(message) {
    // Convert markdown-style formatting to HTML
    let formatted = message;

    // Handle section headers with ** formatting first
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Split into lines for better processing
    let lines = formatted.split('\n');
    let result = [];
    let inList = false;
    let currentListType = 'ul';

    for (let i = 0; i < lines.length; i++) {
        let line = lines[i].trim();

        // Skip empty lines
        if (!line) {
            if (inList) {
                result.push(`</${currentListType}>`);
                inList = false;
            }
            result.push('<br>');
            continue;
        }

        // Check for meaningful numbered lists FIRST
        if (line.match(/^\d+\.\s+(.+)$/)) {
            let content = line.replace(/^\d+\.\s+/, '');

            if (!inList) {
                result.push('<ol>');
                inList = true;
                currentListType = 'ol';
            } else if (currentListType !== 'ol') {
                result.push(`</${currentListType}>`);
                result.push('<ol>');
                currentListType = 'ol';
            }

            result.push(`<li>${content}</li>`);
            continue;
        }

        // Check for bullet points
        if (line.match(/^[•\-]\s+(.*)$/)) {
            let content = line.replace(/^[•\-]\s+/, '');

            if (!inList) {
                result.push('<ul>');
                inList = true;
                currentListType = 'ul';
            } else if (currentListType !== 'ul') {
                result.push(`</${currentListType}>`);
                result.push('<ul>');
                currentListType = 'ul';
            }

            result.push(`<li>${content}</li>`);
            continue;
        }

        // Handle section headers
        if (line.match(/^[🔥💼🏖️👶⚖️💻🏠🔄📚📋📞🍽️📱🤒🌟🗳️⚠️🏋️🎓]/)) {
            if (inList) {
                result.push(`</${currentListType}>`);
                inList = false;
            }
            result.push(`<h3>${line}</h3>`);
            continue;
        }

        // Skip meaningless content but keep simple greetings and responses
        if (line.match(/^(\d+)$/) || line.match(/^[•\-]$/) ||
            (line.length < 3 && !line.match(/^(hi|ok|no|yes|bye)$/i))) {
            continue;
        }

        // Remove unnecessary standalone numbers
        if (line.match(/^\d+\s+/) && !line.match(/^\d+\.\s+/)) {
            line = line.replace(/^\d+\s+/, '');
        }

        // Regular paragraphs
        if (inList) {
            result.push(`</${currentListType}>`);
            inList = false;
        }

        if (!line.includes('<strong>') && !line.includes('<h3>')) {
            result.push(`<p>${line}</p>`);
        } else {
            result.push(line);
        }
    }

    // Close any remaining list
    if (inList) {
        result.push(`</${currentListType}>`);
    }

    return result.join('');
}

function showProfessionalTypingIndicator() {
    const typingIndicator = document.getElementById('typing-indicator');
    if (typingIndicator) {
        typingIndicator.style.display = 'block';

        // Smooth scroll to show the typing indicator
        setTimeout(() => {
            typingIndicator.scrollIntoView({
                behavior: 'smooth',
                block: 'nearest'
            });
        }, 100);
    }
}

function hideProfessionalTypingIndicator() {
    const typingIndicator = document.getElementById('typing-indicator');
    if (typingIndicator) {
        typingIndicator.style.display = 'none';
    }
}

function hideWelcomeSection() {
    const welcomeSection = document.getElementById('welcome-section');
    if (welcomeSection && messageCount === 1) {
        welcomeSection.style.display = 'none';
    }
}

// Add preset question functionality
function addPresetQuestion(question) {
    const input = document.getElementById('message-input');
    input.value = question;
    input.focus();

    // Trigger character count update
    const event = new Event('input');
    input.dispatchEvent(event);
}

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // Ctrl/Cmd + K to focus input
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        document.getElementById('message-input').focus();
    }
});

// Initialize app
console.log('🚀 Professional Health Catalyst Chatbot initialized successfully!');
