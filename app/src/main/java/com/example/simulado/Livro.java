package com.example.simulado;

import com.google.firebase.firestore.Exclude;

public class Livro {
    @Exclude
    private String id;
    private String titulo;
    private String autor;
    private String genero;
    private double ano;
    private String status;
    private boolean favorito;

    public Livro() {}

    public Livro(String id, String titulo, String autor, String genero, double ano, String status, boolean favorito) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.genero = genero;
        this.ano = ano;
        this.status = status;
        this.favorito = favorito;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public double getAno() {
        return ano;
    }

    public void setAno(double ano) {
        this.ano = ano;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }
}