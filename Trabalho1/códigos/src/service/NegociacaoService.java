package service;

import model.Loja;
import model.Produto;
import model.Trocavel;

public class NegociacaoService {
    private Loja loja;

    public NegociacaoService(Loja loja) {
        this.loja = loja;
    }

    public String realizarVenda(int idProduto) {
        Produto p = loja.getAcervo().stream()
                .filter(item -> item.getId() == idProduto)
                .findFirst()
                .orElse(null);

        if (p == null) {
            return "ERRO: Produto ID " + idProduto + " não encontrado no acervo.";
        }
        loja.removerProduto(idProduto);
        return String.format("VENDA CONCLUÍDA: '%s' vendido por R$ %.2f.", p.getTitulo(), p.getPreco());
    }

    public String avaliarTroca(int idProduto) {
        Produto p = loja.getAcervo().stream()
                .filter(item -> item.getId() == idProduto)
                .findFirst()
                .orElse(null);

        if (p == null) return "ERRO: Produto não encontrado.";
        if (p instanceof Trocavel) {
            Trocavel t = (Trocavel) p;
            if (t.podeTrocar()) {
                return String.format("TROCA APROVADA: Crédito de R$ %.2f para '%s'.", t.calcularValorTroca(), p.getTitulo());
            }
            return "TROCA RECUSADA: Estado de conservação não permite troca.";
        }
        return "TROCA RECUSADA: Produtos do tipo " + p.getTipo() + " (ex: e-books) não são trocáveis.";
    }
}
