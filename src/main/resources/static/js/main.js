// =============================================
// ServiceMatch - main.js
// Funcoes globais reutilizadas em todas as paginas
// =============================================

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
