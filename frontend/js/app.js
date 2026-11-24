(() => {
  'use strict';

  const FUSO = 'America/Maceio';
  const app = document.getElementById('app');
  const aviso = document.getElementById('aviso');
  let timer = null;

  const guardar = {
    ler(k) { try { return sessionStorage.getItem(k); } catch { return null; } },
    gravar(k, v) { try { sessionStorage.setItem(k, v); } catch { } },
    apagar(k) { try { sessionStorage.removeItem(k); } catch { } },
  };
  let sessao = null;
  try { sessao = JSON.parse(guardar.ler('sessao') || 'null'); } catch { sessao = null; }

  function h(tag, attrs, ...filhos) {
    const el = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs || {})) {
      if (v === false || v == null) continue;
      if (k === 'class') el.className = v;
      else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
      else if (k === 'text') el.textContent = v;
      else el.setAttribute(k, v === true ? '' : v);
    }
    for (const f of filhos.flat()) {
      if (f == null || f === false) continue;
      el.append(f.nodeType ? f : document.createTextNode(String(f)));
    }
    return el;
  }

  function avisar(texto, ruim = false) {
    const caixa = h('div', { class: ruim ? 'ruim' : '' }, texto);
    aviso.replaceChildren(caixa);
    setTimeout(() => { if (caixa.isConnected) caixa.remove(); }, ruim ? 6000 : 3500);
  }

  const hoje = () => new Intl.DateTimeFormat('en-CA', { timeZone: FUSO }).format(new Date());
  const mesAtual = () => hoje().slice(0, 7);
  const horaDe = (iso) => new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  const dataLonga = (dia) => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full', timeZone: 'UTC' }).format(new Date(dia + 'T00:00:00Z'));

  class ErroApi extends Error {
    constructor(status, mensagem) { super(mensagem); this.status = status; }
  }

  async function api(caminho, { method = 'GET', corpo } = {}) {
    let res;
    try {
      res = await fetch('/api' + caminho, {
        method,
        headers: {
          ...(corpo ? { 'Content-Type': 'application/json' } : {}),
          ...(sessao ? { Authorization: 'Bearer ' + sessao.token } : {}),
        },
        body: corpo ? JSON.stringify(corpo) : undefined,
      });
    } catch {
      throw new ErroApi(0, 'Não foi possível falar com o servidor. Verifique a conexão e tente de novo.');
    }
    let dados = null;
    try { dados = await res.json(); } catch { }
    if (res.status === 401 && caminho !== '/login') {
      sair('Sua sessão expirou. Entre novamente.');
      throw new ErroApi(401, 'Sessão expirada');
    }
    if (!res.ok) {
      const padrao = { 400: 'Confira os dados informados.', 403: 'Você não tem acesso a isso.', 404: 'Não encontrado.', 409: 'Esses dados já existem ou conflitam com outro cadastro.' }[res.status]
        || 'O servidor não conseguiu concluir a ação.';
      throw new ErroApi(res.status, (dados && (dados.mensagem || dados.erro)) || padrao);
    }
    return dados;
  }

  let avisoLogin = '';

  function sair(mensagem = '') {
    sessao = null;
    guardar.apagar('sessao');
    avisoLogin = mensagem;
    if (location.hash) location.hash = ''; else rotear();
  }

  function rotear() {
    clearInterval(timer);
    timer = null;
    if (!sessao) return telaLogin();
    const [, rota, id] = location.hash.split('/');
    if (rota === 'turma' && id) return telaTurma(Number(id));
    if (rota === 'relatorio') return telaRelatorio();
    return telaTurmas();
  }
  window.addEventListener('hashchange', rotear);

  function quadro(ativa, conteudo) {
    const link = (href, texto, chave) => h('a', { href, 'aria-current': ativa === chave ? 'page' : false }, texto);
    app.replaceChildren(
      h('header', { class: 'topo' }, h('div', { class: 'topo-in' },
        h('span', { class: 'nome' }, 'Chamada'),
        h('nav', { 'aria-label': 'Principal' },
          link('#/turmas', 'Turmas', 'turmas'),
          link('#/relatorio', 'Relatório', 'relatorio')),
        h('span', { class: 'quem' }, sessao.nome),
        h('button', { class: 'btn', type: 'button', onclick: () => sair() }, 'Sair'))),
      h('main', {}, conteudo));
    document.title = 'Chamada';
  }

  function telaLogin() {
    document.title = 'Entrar · Chamada';
    const padrao = [1,0,1,1,0,1,1,1, 1,1,0,1,1,0,1,0, 0,1,1,1,1,1,0,1, 1,0,1,0,1,1,1,1, 1,1,1,0,1,0,1,1];
    const mural = h('div', { class: 'mural', 'aria-hidden': 'true' },
      padrao.map((on, i) => h('span', { class: 'marca' + (on ? ' on' : ''), style: `--i:${i}` })));

    const erro = h('p', { class: 'erro', role: 'alert' }, avisoLogin);
    avisoLogin = '';
    const botao = h('button', { class: 'btn cheio', type: 'submit' }, 'Entrar');
    const nome = h('input', { id: 'nome', name: 'nome', autocomplete: 'username', required: true, autofocus: true });
    const senha = h('input', { id: 'senha', name: 'senha', type: 'password', autocomplete: 'current-password', required: true });

    const form = h('form', {
      onsubmit: async (e) => {
        e.preventDefault();
        erro.textContent = '';
        botao.disabled = true;
        try {
          const r = await api('/login', { method: 'POST', corpo: { nome: nome.value, senha: senha.value } });
          sessao = r;
          guardar.gravar('sessao', JSON.stringify(r));
          if (location.hash === '#/turmas') rotear(); else location.hash = '#/turmas';
        } catch (err) {
          erro.textContent = err.message;
          botao.disabled = false;
          senha.select();
        }
      },
    },
      h('h2', {}, 'Entrar'),
      h('p', { class: 'sub' }, 'Use o usuário e a senha do seu cadastro.'),
      h('div', { class: 'campo' }, h('label', { for: 'nome' }, 'Usuário'), nome),
      h('div', { class: 'campo' }, h('label', { for: 'senha' }, 'Senha'), senha),
      erro, botao);

    app.replaceChildren(h('div', { class: 'login' },
      h('section', { class: 'login-arte' },
        h('div', {}, h('h1', {}, 'Chamada'), h('p', {}, 'A presença da turma aparece aqui assim que o cartão é aproximado do leitor.')),
        mural),
      h('section', { class: 'login-form' }, form)));
  }

  async function telaTurmas() {
    quadro('turmas', h('p', { class: 'sub' }, 'Carregando turmas…'));
    try {
      const turmas = await api('/turmas');
      const conteudo = [h('div', { class: 'cabeca' }, h('div', {}, h('h2', {}, 'Minhas turmas'), h('p', { class: 'sub' }, `Olá, ${sessao.nome}.`)))];
      if (!turmas.length) {
        conteudo.push(h('div', { class: 'vazio-tela' },
          h('p', {}, 'Nenhuma turma está ligada ao seu usuário.'),
          h('p', {}, 'Peça ao administrador para associar suas turmas ao seu cadastro.')));
      } else {
        conteudo.push(h('ul', { class: 'turmas' }, turmas.map((t) =>
          h('li', {}, h('a', { href: '#/turma/' + t.id_turma },
            h('span', { class: 't' }, [t.nome, t.turma].filter(Boolean).join(' ')),
            h('span', { class: 'ano' }, t.ano))))));
      }
      quadro('turmas', conteudo);
    } catch (e) {
      if (e.status !== 401) quadro('turmas', h('p', { class: 'erro' }, e.message));
    }
  }

  async function telaTurma(id) {
    const estado = { dia: hoje(), aula: null, filtro: 'todos', alunos: [], aulas: [], turma: null, porAluno: new Map() };
    const lista = h('div');
    const topo = h('div');
    let montada = false;

    const agoraHM = () => new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date());

    function aulaPadrao() {
      if (estado.dia !== hoje() || !estado.aulas.length) return 'todas';
      const agora = agoraHM();
      if (agora > estado.aulas[estado.aulas.length - 1].fim) return 'todas';
      const iniciadas = estado.aulas.filter((a) => a.inicio <= agora);
      return iniciadas.length ? iniciadas[iniciadas.length - 1].numero : 'todas';
    }

    const horaNaAula = (matricula, aula) => {
      const m = estado.porAluno.get(matricula);
      return m ? (m.get(aula) || m.get(0) || null) : null;
    };
    const aulasDoAluno = (matricula) => estado.aulas.length
      ? estado.aulas.filter((a) => horaNaAula(matricula, a.numero)).length
      : (estado.porAluno.has(matricula) ? 1 : 0);

    async function carregar(silencioso) {
      try {
        const [alunos, presencas, turmas, aulas] = await Promise.all([
          api(`/alunos/turma/${id}`),
          api(`/presencas/turma/${id}?dia=${estado.dia}`),
          estado.turma ? Promise.resolve(null) : api('/turmas'),
          estado.aulas.length ? Promise.resolve(null) : api('/aulas'),
        ]);
        if (turmas) estado.turma = turmas.find((t) => t.id_turma === id) || { nome: 'Turma', turma: '', ano: '' };
        if (aulas) estado.aulas = aulas;
        if (estado.aula === null) estado.aula = aulaPadrao();
        estado.alunos = alunos.sort((a, b) => a.nome_aluno.localeCompare(b.nome_aluno, 'pt-BR'));
        estado.porAluno = new Map();
        for (const p of presencas) {
          if (p.situacao && p.situacao.toUpperCase() !== 'PRESENTE') continue;
          const m = estado.porAluno.get(p.matricula_aluno) || new Map();
          const chave = p.numero_aula == null ? 0 : p.numero_aula;
          const atual = m.get(chave);
          if (!atual || p.data_hora < atual) m.set(chave, p.data_hora);
          estado.porAluno.set(p.matricula_aluno, m);
        }
        desenhar();
      } catch (e) {
        if (e.status === 401) return;
        if (location.hash !== '#/turma/' + id) return;
        if (silencioso) return avisar('Não foi possível atualizar a lista: ' + e.message, true);
        quadro('turmas', [h('a', { class: 'voltar', href: '#/turmas' }, '← Turmas'), h('p', { class: 'erro' }, e.message)]);
      }
    }

    function desenhar() {
      if (location.hash !== '#/turma/' + id) return;
      if (!montada) {
        const nomeTurma = [estado.turma.nome, estado.turma.turma].filter(Boolean).join(' ');
        const data = h('input', {
          type: 'date', id: 'dia', value: estado.dia, max: hoje(),
          onchange: (e) => {
            if (!e.target.value) return;
            estado.dia = e.target.value;
            estado.aula = aulaPadrao();
            carregar();
            agendar();
          },
        });
        quadro('turmas', [
          h('a', { class: 'voltar', href: '#/turmas' }, '← Turmas'),
          h('div', { class: 'cabeca' },
            h('div', {}, h('h2', {}, nomeTurma), h('p', { class: 'sub' }, estado.turma.ano ? 'Ano letivo ' + estado.turma.ano : '')),
            h('div', { class: 'data-campo' }, h('label', { for: 'dia' }, 'Dia'), data)),
          topo, lista]);
        document.title = nomeTurma + ' · Chamada';
        montada = true;
      }

      const total = estado.alunos.length;
      const porAula = estado.aula !== 'todas' && estado.aulas.length;
      const aulaSel = porAula ? estado.aulas.find((a) => a.numero === estado.aula) : null;
      const ehHoje = estado.dia === hoje();
      const estaPresente = (a) => (aulaSel
        ? !!horaNaAula(a.matricula_aluno, aulaSel.numero)
        : aulasDoAluno(a.matricula_aluno) > 0);
      const presentes = estado.alunos.filter(estaPresente).length;

      const chip = (valor, rotulo, dica) => h('button', {
        type: 'button', title: dica || false, 'aria-pressed': String(estado.aula === valor),
        onclick: () => { estado.aula = valor; desenhar(); },
      }, rotulo);

      topo.replaceChildren(
        h('section', { class: 'resumo', 'aria-label': 'Resumo do dia' },
          h('p', { class: 'num' }, `${presentes} de ${total}`,
            h('small', {}, aulaSel ? `presentes na ${aulaSel.nome}` : 'com presença em alguma aula')),
          h('p', { class: 'sub' }, dataLonga(estado.dia) + (ehHoje ? ' · atualiza sozinho' : '')),
          aulaSel ? h('p', { class: 'sub' }, `${aulaSel.inicio}–${aulaSel.fim} · o cartão vale das ${aulaSel.janelaInicio} às ${aulaSel.fim}`) : null,
          total ? h('div', { class: 'pontos', 'aria-hidden': 'true' },
            estado.alunos.map((a) => h('span', { class: 'marca' + (estaPresente(a) ? ' on' : '') }))) : null),
        estado.aulas.length ? h('div', { class: 'filtros aulas', role: 'group', 'aria-label': 'Aula' },
          chip('todas', 'Todas as aulas'),
          estado.aulas.map((a) => chip(a.numero, a.nome, `${a.inicio}–${a.fim}`))) : null);

      const filtro = (chave, rotulo) => h('button', {
        type: 'button', 'aria-pressed': String(estado.filtro === chave),
        onclick: () => { estado.filtro = chave; desenhar(); },
      }, rotulo);

      const visiveis = estado.alunos.filter((a) => {
        const p = estaPresente(a);
        return estado.filtro === 'todos' || (estado.filtro === 'presentes' ? p : !p);
      });

      const barra = h('div', { class: 'barra' },
        h('div', { class: 'filtros', role: 'group', 'aria-label': 'Filtrar alunos' },
          filtro('todos', 'Todos'), filtro('presentes', 'Presentes'), filtro('ausentes', 'Ausentes')),
        h('button', { class: 'btn', type: 'button', onclick: () => carregar(true).then(() => avisar('Lista atualizada.')) }, 'Atualizar'),
        sessao.admin ? h('button', { class: 'btn cheio', type: 'button', onclick: novoAluno }, 'Novo aluno') : null);

      let corpo;
      if (!total) {
        corpo = h('div', { class: 'vazio-tela' }, h('p', {}, 'Esta turma ainda não tem alunos.'),
          sessao.admin ? h('button', { class: 'btn cheio', type: 'button', onclick: novoAluno }, 'Cadastrar o primeiro aluno')
            : h('p', {}, 'O administrador cadastra os alunos da turma.'));
      } else if (!visiveis.length) {
        corpo = h('p', { class: 'sub', style: 'padding:16px 4px' }, 'Nenhum aluno neste filtro.');
      } else {
        corpo = h('ul', { class: 'chamada' }, visiveis.map((a) => {
          const temCartao = a.uid && a.uid.trim();
          const presente = estaPresente(a);
          let direita;
          if (aulaSel) {
            const hora = horaNaAula(a.matricula_aluno, aulaSel.numero);
            direita = h('span', { class: 'hora' + (hora ? '' : ' vazio') }, hora ? horaDe(hora) : '—');
          } else {
            const n = aulasDoAluno(a.matricula_aluno);
            direita = h('span', { class: 'hora-aulas' },
              h('span', { class: 'mini', 'aria-hidden': 'true' },
                estado.aulas.map((x) => h('span', { class: 'marca' + (horaNaAula(a.matricula_aluno, x.numero) ? ' on' : '') }))),
              h('span', { class: 'hora' + (n ? '' : ' vazio') }, estado.aulas.length ? `${n}/${estado.aulas.length}` : (n ? 'Presente' : '—')));
          }
          return h('li', { class: presente ? 'presente' : '' },
            h('span', { class: 'marca' + (presente ? ' on' : ''), role: 'img', 'aria-label': presente ? 'Presente' : 'Ausente' }),
            h('div', { class: 'quem' }, h('div', { class: 'aluno' }, a.nome_aluno), h('div', { class: 'mat' }, 'Matrícula ' + a.matricula_aluno)),
            direita,
            temCartao
              ? h('span', { class: 'cartao' }, 'Cartão cadastrado')
              : sessao.admin
                ? h('button', { class: 'btn link cartao', type: 'button', onclick: () => novoCartao(a) }, 'Associar cartão')
                : h('span', { class: 'cartao' }, 'Sem cartão'));
        }));
      }
      lista.replaceChildren(barra, corpo);
    }

    function novoAluno() {
      dialogo({
        titulo: 'Novo aluno', rotulo: 'Cadastrar',
        campos: [
          { nome: 'matricula', rotulo: 'Matrícula', tipo: 'number', dica: 'Número inteiro até 2147483647. Precisa ser nova.' },
          { nome: 'nome', rotulo: 'Nome do aluno', tipo: 'text' },
          { nome: 'uid', rotulo: 'Cartão (opcional)', tipo: 'text', obrigatorio: false, dica: 'UID do cartão. Pode ficar em branco e ser associado depois.' },
        ],
        async enviar(v) {
          const matricula = Number(v.matricula);
          if (!Number.isInteger(matricula) || matricula < 1 || matricula > 2147483647) throw new Error('A matrícula precisa ser um número inteiro entre 1 e 2147483647.');
          await api('/alunos', { method: 'POST', corpo: { matricula, idTurma: id, nome: v.nome.trim() } });
          if (v.uid.trim()) {
            try { await api('/cartoes', { method: 'POST', corpo: { matricula, uid: v.uid.trim() } }); }
            catch (e) { avisar('Aluno cadastrado, mas o cartão não foi associado: ' + e.message, true); await carregar(true); return; }
          }
          avisar('Aluno cadastrado.');
          await carregar(true);
        },
      });
    }

    function novoCartao(aluno) {
      dialogo({
        titulo: 'Associar cartão', descricao: aluno.nome_aluno + ' · matrícula ' + aluno.matricula_aluno, rotulo: 'Associar',
        campos: [{ nome: 'uid', rotulo: 'UID do cartão', tipo: 'text', dica: 'O UID não pode estar em uso por outro aluno.' }],
        async enviar(v) {
          await api('/cartoes', { method: 'POST', corpo: { matricula: aluno.matricula_aluno, uid: v.uid.trim() } });
          avisar('Cartão associado.');
          await carregar(true);
        },
      });
    }

    function agendar() {
      clearInterval(timer);
      timer = estado.dia === hoje() ? setInterval(() => carregar(true), 20000) : null;
    }

    quadro('turmas', h('p', { class: 'sub' }, 'Carregando turma…'));
    await carregar();
    agendar();
  }


  function telaRelatorio() {
    const saida = h('div');
    const mes = h('input', { type: 'month', id: 'mes', value: mesAtual(), required: true });
    const botao = h('button', { class: 'btn cheio', type: 'submit' }, 'Atualizar relatório');
    const form = h('form', {
      class: 'barra',
      onsubmit: async (e) => {
        e.preventDefault();
        botao.disabled = true;
        saida.replaceChildren(h('p', { class: 'sub' }, 'Atualizando a planilha. Isso pode levar alguns segundos…'));
        try {
          const r = await api('/relatorios/mensal?mes=' + encodeURIComponent(mes.value), { method: 'POST' });
          saida.replaceChildren(h('div', { class: 'resultado' },
            h('p', {}, h('strong', {}, `Relatório de ${r.mes} atualizado às ${horaDe(new Date().toISOString())}.`)),
            h('p', {}, `${r.abas.length} ${r.abas.length === 1 ? 'aba atualizada' : 'abas atualizadas'}` +
              (r.abasCriadas ? `, ${r.abasCriadas} ${r.abasCriadas === 1 ? 'criada' : 'criadas'} agora` : '') +
              (r.turmasSemAlunos ? `. ${r.turmasSemAlunos} ${r.turmasSemAlunos === 1 ? 'turma sem alunos ficou' : 'turmas sem alunos ficaram'} de fora` : '') + '.'),
            h('a', { href: r.planilha, target: '_blank', rel: 'noopener' }, 'Abrir a planilha')));
        } catch (err) {
          if (err.status !== 401) saida.replaceChildren(h('p', { class: 'erro' }, err.message));
        } finally { botao.disabled = false; }
      },
    }, h('div', { class: 'data-campo' }, h('label', { for: 'mes' }, 'Mês'), mes), botao);

    quadro('relatorio', [
      h('div', { class: 'cabeca' }, h('div', {}, h('h2', {}, 'Relatório mensal'),
        h('p', { class: 'sub' }, 'Grava na planilha do Google a presença de todas as turmas no mês escolhido. Se o relatório já existe, ele é atualizado com as leituras mais recentes.'))),
      form, saida]);
  }

  function dialogo({ titulo, descricao, campos, rotulo, enviar }) {
    const erro = h('p', { class: 'erro', role: 'alert' });
    const inputs = {};
    const dlg = h('dialog', { 'aria-labelledby': 'dlg-titulo' });
    const botao = h('button', { class: 'btn cheio', type: 'submit' }, rotulo);
    const form = h('form', {
      method: 'dialog',
      onsubmit: async (e) => {
        e.preventDefault();
        erro.textContent = '';
        botao.disabled = true;
        try {
          await enviar(Object.fromEntries(Object.entries(inputs).map(([k, i]) => [k, i.value])));
          dlg.close();
        } catch (err) {
          if (err.status !== 401) { erro.textContent = err.message; botao.disabled = false; }
        }
      },
    },
      h('h2', { id: 'dlg-titulo' }, titulo),
      descricao ? h('p', { class: 'sub', style: 'color:var(--suave)' }, descricao) : null,
      campos.map((c, i) => {
        const input = h('input', { id: 'c-' + c.nome, type: c.tipo, required: c.obrigatorio !== false, autocomplete: 'off', min: c.tipo === 'number' ? 1 : false });
        inputs[c.nome] = input;
        return h('div', { class: 'campo' }, h('label', { for: 'c-' + c.nome }, c.rotulo), input, c.dica ? h('span', { class: 'dica' }, c.dica) : null);
      }),
      erro,
      h('div', { class: 'acoes' },
        h('button', { class: 'btn', type: 'button', onclick: () => dlg.close() }, 'Cancelar'), botao));
    dlg.append(form);
    dlg.addEventListener('close', () => dlg.remove());
    document.body.append(dlg);
    dlg.showModal();
    Object.values(inputs)[0].focus();
  }

  rotear();
})();
