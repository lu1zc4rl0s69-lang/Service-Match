// =============================================
// ServiceMatch - admin-avaliacoes.js
// JS especifico da pagina de avaliacoes (admin)
// =============================================

function confirmarExclusao(id) {
    const form = document.getElementById('formExcluir');
    form.action = '/admin/avaliacoes/' + id + '/excluir';
    openModal('modalExcluir');
}
