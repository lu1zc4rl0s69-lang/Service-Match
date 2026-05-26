// =============================================
// ServiceMatch - avaliacoes.js
// JS especifico da pagina de avaliacoes (cliente)
// =============================================

function openAval(id, titulo) {
    document.getElementById('avalChamadoId').value = id;
    document.getElementById('avalTitulo').value = titulo;
    openModal('modalAval');
}

function setStars(el) {
    const val = parseInt(el.id.replace('star', ''));
    for (let i = 1; i <= 5; i++) {
        const s = document.getElementById('star' + i);
        s.className = i <= val ? 'bi bi-star-fill' : 'bi bi-star';
        s.style.color = i <= val ? '#f59e0b' : '#d1d5db';
        s.parentElement.querySelector('input').checked = (i === val);
    }
}
