package com.example.simulado;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LivroAdapter extends RecyclerView.Adapter<LivroAdapter.ViewHolder> {

    private List<Livro> lista;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(Livro item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Livro item);
    }

    public LivroAdapter(List<Livro> lista) {
        this.lista = lista;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void atualizarLista(List<Livro> novaLista) {
        this.lista = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_livro, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Livro item = lista.get(position);

        holder.txtTitulo.setText(item.getTitulo());
        holder.txtAutorGenero.setText("Autor: " + item.getAutor() + " • Gênero: " + item.getGenero());
        
        int anoFormatado = (int) item.getAno();
        holder.txtAnoStatus.setText("Ano: " + anoFormatado + " | Status: " + item.getStatus());
        holder.txtFavorito.setText(item.isFavorito() ? "★ Favorito" : "");

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
                return true;
            }
            return false;
        });

        if (holder.btnDeletar != null) {
            holder.btnDeletar.setOnClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitulo, txtAutorGenero, txtAnoStatus, txtFavorito;
        ImageButton btnDeletar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTituloItem);
            txtAutorGenero = itemView.findViewById(R.id.txtAutorGeneroItem);
            txtAnoStatus = itemView.findViewById(R.id.txtAnoStatusItem);
            txtFavorito = itemView.findViewById(R.id.txtFavoritoItem);
            btnDeletar = itemView.findViewById(R.id.btnDeletarItem);
        }
    }
}