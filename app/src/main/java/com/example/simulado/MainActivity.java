package com.example.simulado;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    // Campos do formulário
    private EditText edtTitulo, edtAutor, edtAno;
    private Spinner spnGenero, spnStatus;
    private CheckBox chkFavorito;
    private Button btnSalvar, btnCancelar;

    // Campos de busca, filtro e contador
    private EditText edtBusca;
    private Spinner spnFiltroGenero;
    private CheckBox chkFiltroFavoritos;
    private TextView txtTotalLivros;

    // Listas e Adapter
    private List<Livro> listaCompleta = new ArrayList<>();
    private List<Livro> listaExibida = new ArrayList<>();
    private LivroAdapter adapter;
    private Livro livroEditando = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        // Mapeamento dos elementos da UI
        edtTitulo = findViewById(R.id.edtTitulo);
        edtAutor = findViewById(R.id.edtAutor);
        spnGenero = findViewById(R.id.spnGenero);
        edtAno = findViewById(R.id.edtAno);
        spnStatus = findViewById(R.id.spnStatus);
        chkFavorito = findViewById(R.id.chkFavorito);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnCancelar = findViewById(R.id.btnCancelar);

        edtBusca = findViewById(R.id.edtBusca);
        spnFiltroGenero = findViewById(R.id.spnFiltroGenero);
        chkFiltroFavoritos = findViewById(R.id.chkFiltroFavoritos);
        txtTotalLivros = findViewById(R.id.txtTotalLivros);

        RecyclerView recyclerLivros = findViewById(R.id.recyclerLivros);
        recyclerLivros.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LivroAdapter(listaExibida);
        recyclerLivros.setAdapter(adapter);

        // Listeners dos botões
        btnSalvar.setOnClickListener(v -> salvarLivro());
        btnCancelar.setOnClickListener(v -> limparCampos());

        // Listeners da lista (clique curto e longo)
        adapter.setOnItemClickListener(this::carregarParaEdicao);
        adapter.setOnItemLongClickListener(this::confirmarExclusao);

        // Listeners dos filtros e busca
        spnFiltroGenero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltrosEBusca();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        chkFiltroFavoritos.setOnCheckedChangeListener((buttonView, isChecked) -> aplicarFiltrosEBusca());

        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltrosEBusca();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Garantir que os filtros comecem limpos ao abrir o aplicativo
        spnFiltroGenero.setSelection(0);
        chkFiltroFavoritos.setChecked(false);
        edtBusca.setText("");

        carregarLivros();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void carregarLivros() {
        listaCompleta.clear();

        // Busca na coleção "livros"
        db.collection("livros")
                .get()
                .addOnSuccessListener(snapshotsLivros -> {
                    processarDocumentos(snapshotsLivros);

                    // Busca também na coleção "filmes_series" caso haja cadastros anteriores
                    db.collection("filmes_series")
                            .get()
                            .addOnSuccessListener(snapshotsFilmes -> {
                                processarDocumentos(snapshotsFilmes);
                                aplicarFiltrosEBusca();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FIREBASE", "Erro ao carregar coleção filmes_series", e);
                                aplicarFiltrosEBusca();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "Erro ao carregar coleção livros", e);
                    Toast.makeText(this, "Erro ao carregar lista de itens", Toast.LENGTH_SHORT).show();
                });
    }

    private void processarDocumentos(Iterable<QueryDocumentSnapshot> queryDocumentSnapshots) {
        if (queryDocumentSnapshots == null) return;

        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
            // Verificar se o documento já está na lista para não duplicar
            boolean jaExiste = false;
            for (Livro l : listaCompleta) {
                if (l.getId() != null && l.getId().equals(doc.getId())) {
                    jaExiste = true;
                    break;
                }
            }
            if (jaExiste) continue;

            String id = doc.getId();

            // Título/Nome
            String titulo = doc.getString("titulo");
            if (titulo == null || titulo.trim().isEmpty()) {
                titulo = doc.getString("nome");
            }
            if (titulo == null || titulo.trim().isEmpty()) {
                titulo = "Sem título";
            }

            // Autor/Tipo
            String autor = doc.getString("autor");
            if (autor == null || autor.trim().isEmpty()) {
                autor = doc.getString("tipo");
            }
            if (autor == null || autor.trim().isEmpty()) {
                autor = "Autor não informado";
            }

            // Gênero
            String genero = doc.getString("genero");
            if (genero == null || genero.trim().isEmpty()) {
                genero = "Outros";
            }

            // Ano
            double ano = 0;
            Object anoObj = doc.get("ano");
            if (anoObj instanceof Number) {
                ano = ((Number) anoObj).doubleValue();
            } else {
                Object estoqueObj = doc.get("estoque");
                if (estoqueObj instanceof Number) {
                    ano = ((Number) estoqueObj).doubleValue();
                }
            }

            // Status
            String status = "Quero ler";
            Object statusObj = doc.get("status");
            if (statusObj instanceof String && !((String) statusObj).trim().isEmpty()) {
                status = (String) statusObj;
            } else {
                Boolean assistidoObj = doc.getBoolean("assistido");
                if (assistidoObj != null) {
                    status = assistidoObj ? "Concluído" : "Quero ler";
                }
            }

            // Favorito
            Boolean favObj = doc.getBoolean("favorito");
            if (favObj == null) {
                favObj = doc.getBoolean("assistido");
            }
            boolean favorito = favObj != null && favObj;

            Livro livro = new Livro(id, titulo, autor, genero, ano, status, favorito);
            listaCompleta.add(livro);
        }
    }

    private void aplicarFiltrosEBusca() {
        String textoBusca = edtBusca != null ? edtBusca.getText().toString().toLowerCase().trim() : "";
        int posFiltroGenero = spnFiltroGenero != null ? spnFiltroGenero.getSelectedItemPosition() : 0;
        boolean apenasFavoritos = chkFiltroFavoritos != null && chkFiltroFavoritos.isChecked();

        listaExibida.clear();

        for (Livro livro : listaCompleta) {
            String titulo = livro.getTitulo() != null ? livro.getTitulo().toLowerCase() : "";
            String autor = livro.getAutor() != null ? livro.getAutor().toLowerCase() : "";

            // Filtro por busca de título ou autor
            boolean passaBusca = textoBusca.isEmpty()
                    || titulo.contains(textoBusca)
                    || autor.contains(textoBusca);

            // Filtro por gênero
            boolean passaGenero = (posFiltroGenero == 0);
            if (!passaGenero && spnFiltroGenero != null && spnFiltroGenero.getSelectedItem() != null) {
                String filtroGenero = spnFiltroGenero.getSelectedItem().toString().trim();
                String generoLivro = livro.getGenero() != null ? livro.getGenero().trim() : "";
                passaGenero = filtroGenero.equalsIgnoreCase(generoLivro) || "Todos os gêneros".equalsIgnoreCase(filtroGenero);
            }

            // Filtro por favoritos
            boolean passaFavorito = !apenasFavoritos || livro.isFavorito();

            if (passaBusca && passaGenero && passaFavorito) {
                listaExibida.add(livro);
            }
        }

        adapter.atualizarLista(listaExibida);
        if (txtTotalLivros != null) {
            txtTotalLivros.setText("Total no Firebase: " + listaCompleta.size() + " livro(s) | Exibindo na lista: " + listaExibida.size());
        }
    }

    private void salvarLivro() {
        String titulo = edtTitulo.getText().toString().trim();
        String autor = edtAutor.getText().toString().trim();
        String anoStr = edtAno.getText().toString().trim();
        boolean favorito = chkFavorito.isChecked();

        int posGenero = spnGenero.getSelectedItemPosition();
        int posStatus = spnStatus.getSelectedItemPosition();

        // VALIDAÇÕES MÍNIMAS EXIGIDAS
        if (titulo.isEmpty()) {
            Toast.makeText(this, "Por favor, informe o título do livro", Toast.LENGTH_SHORT).show();
            edtTitulo.requestFocus();
            return;
        }

        if (autor.isEmpty()) {
            Toast.makeText(this, "Por favor, informe o autor do livro", Toast.LENGTH_SHORT).show();
            edtAutor.requestFocus();
            return;
        }

        if (posGenero == 0) {
            Toast.makeText(this, "Por favor, selecione um gênero válido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (anoStr.isEmpty()) {
            Toast.makeText(this, "Por favor, informe o ano de publicação", Toast.LENGTH_SHORT).show();
            edtAno.requestFocus();
            return;
        }

        if (posStatus == 0) {
            Toast.makeText(this, "Por favor, selecione um status de leitura", Toast.LENGTH_SHORT).show();
            return;
        }

        double ano;
        try {
            ano = Double.parseDouble(anoStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ano de publicação inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        String genero = spnGenero.getSelectedItem().toString();
        String status = spnStatus.getSelectedItem().toString();

        if (livroEditando == null) {
            // Inclusão no Firebase Firestore na coleção "livros"
            Livro novoLivro = new Livro(null, titulo, autor, genero, ano, status, favorito);
            db.collection("livros")
                    .add(novoLivro)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erro ao cadastrar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Edição no Firebase Firestore
            livroEditando.setTitulo(titulo);
            livroEditando.setAutor(autor);
            livroEditando.setGenero(genero);
            livroEditando.setAno(ano);
            livroEditando.setStatus(status);
            livroEditando.setFavorito(favorito);

            // Verificar se o documento está na coleção "livros" ou "filmes_series"
            db.collection("livros")
                    .document(livroEditando.getId())
                    .set(livroEditando)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    })
                    .addOnFailureListener(e -> {
                        // Tenta atualizar na coleção "filmes_series" se não encontrou em "livros"
                        db.collection("filmes_series")
                                .document(livroEditando.getId())
                                .set(livroEditando)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "Atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                                    limparCampos();
                                    carregarLivros();
                                })
                                .addOnFailureListener(ex -> {
                                    Toast.makeText(this, "Erro ao atualizar: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
        }
    }

    private void carregarParaEdicao(Livro livro) {
        livroEditando = livro;
        edtTitulo.setText(livro.getTitulo());
        edtAutor.setText(livro.getAutor());
        edtAno.setText(String.valueOf((int) livro.getAno()));
        chkFavorito.setChecked(livro.isFavorito());

        selecionarSpinnerPorValor(spnGenero, livro.getGenero());
        selecionarSpinnerPorValor(spnStatus, livro.getStatus());

        btnSalvar.setText("Atualizar");
        btnCancelar.setVisibility(View.VISIBLE);
    }

    private void selecionarSpinnerPorValor(Spinner spinner, String valor) {
        if (spinner == null || valor == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(valor)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void confirmarExclusao(Livro livro) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Item")
                .setMessage("Deseja realmente excluir \"" + livro.getTitulo() + "\"?")
                .setPositiveButton("Excluir", (dialog, which) -> deletarLivro(livro))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarLivro(Livro livro) {
        if (livro == null || livro.getId() == null) return;

        db.collection("livros")
                .document(livro.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Excluído com sucesso!", Toast.LENGTH_SHORT).show();
                    if (livroEditando != null && livro.getId().equals(livroEditando.getId())) {
                        limparCampos();
                    }
                    carregarLivros();
                })
                .addOnFailureListener(e -> {
                    // Tenta excluir da coleção "filmes_series"
                    db.collection("filmes_series")
                            .document(livro.getId())
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Excluído com sucesso!", Toast.LENGTH_SHORT).show();
                                if (livroEditando != null && livro.getId().equals(livroEditando.getId())) {
                                    limparCampos();
                                }
                                carregarLivros();
                            })
                            .addOnFailureListener(ex -> {
                                Toast.makeText(this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void limparCampos() {
        edtTitulo.setText("");
        edtAutor.setText("");
        edtAno.setText("");
        spnGenero.setSelection(0);
        spnStatus.setSelection(0);
        chkFavorito.setChecked(false);

        livroEditando = null;
        btnSalvar.setText("Salvar Livro");
        btnCancelar.setVisibility(View.GONE);
    }
}