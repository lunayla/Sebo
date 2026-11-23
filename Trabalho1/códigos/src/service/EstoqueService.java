package service;

import model.Loja;
import model.Produto;
import java.util.List;
import java.util.stream.Collectors;

public class EstoqueService {
    private Loja loja;

    public EstoqueService(Loja loja) {
        this.loja = loja;
    }

    public void cadastrarProduto(Produto p) {
        loja.adicionarProduto(p);
    }

    public Produto buscarPorId(int id) {
        return loja.getAcervo().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Produto> buscarPorTitulo(String termo) {
        return loja.getAcervo().stream()
                .filter(p -> p.getTitulo().toLowerCase().contains(termo.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Produto> listarTodos() {
        return loja.getAcervo();
    }
}
