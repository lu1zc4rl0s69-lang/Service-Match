// =============================================
// ServiceMatch - main.js
// Funcoes globais reutilizadas em todas as paginas
// =============================================

// ── Tema (claro / escuro) ────────────────────────────────
(function () {
    var t = localStorage.getItem('sm-theme') || 'light';
    document.documentElement.setAttribute('data-theme', t);
})();

function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    var icon  = document.getElementById('themeIcon');
    var label = document.getElementById('themeLabel');
    var cb    = document.getElementById('themeCheckbox');
    var statusLabel = document.getElementById('themeStatusLabel');
    if (theme === 'dark') {
        if (icon)  icon.className   = 'bi bi-sun-fill';
        if (label) label.textContent = 'Tema Claro';
        if (cb)    cb.checked = true;
        if (statusLabel) statusLabel.textContent = 'Escuro';
    } else {
        if (icon)  icon.className   = 'bi bi-moon-fill';
        if (label) label.textContent = 'Tema Escuro';
        if (cb)    cb.checked = false;
        if (statusLabel) statusLabel.textContent = 'Claro';
    }
}

function toggleTheme() {
    var current = document.documentElement.getAttribute('data-theme') || 'light';
    var next = current === 'dark' ? 'light' : 'dark';
    localStorage.setItem('sm-theme', next);
    applyTheme(next);
}

// Sidebar toggle
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const main = document.getElementById('mainContent');
    const icon = document.getElementById('toggleIcon');
    sidebar.classList.toggle('collapsed');
    if (main) main.classList.toggle('collapsed');
    icon.className = sidebar.classList.contains('collapsed')
        ? 'bi bi-chevron-right' : 'bi bi-chevron-left';
}

// Modais
function openModal(id) {
    document.getElementById(id).classList.add('open');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('open');
}

// Inicializar ao carregar a pagina
document.addEventListener('DOMContentLoaded', function () {

    // Sincroniza o ícone do botão de tema com o estado atual
    var t = document.documentElement.getAttribute('data-theme') || 'light';
    applyTheme(t);

    // Fechar modal clicando fora
    document.querySelectorAll('.modal-overlay').forEach(function (overlay) {
        overlay.addEventListener('click', function (e) {
            if (e.target === overlay) overlay.classList.remove('open');
        });
    });

    // Auto-hide alerts apos 5 segundos
    document.querySelectorAll('.alert').forEach(function (alert) {
        setTimeout(function () {
            if (alert.parentNode) alert.remove();
        }, 5000);
    });

});
