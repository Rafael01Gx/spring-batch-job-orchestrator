package br.com.tickets.services;

import br.com.tickets.models.Importacao;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;

public class ImportacaoProcessor implements ItemProcessor<Importacao,Importacao> {
    @Override
    public Importacao process(Importacao item) throws Exception {
            switch (item.getTipoIngresso().toLowerCase()){
                case "vip":
                    item.setTaxaAdmin(BigDecimal.valueOf(150.0));
                    break;
                case "camarote":
                    item.setTaxaAdmin(BigDecimal.valueOf(80.0));
                    break;
                default:
                    item.setTaxaAdmin(BigDecimal.valueOf(50.0));
            }
        return item;
    }
}
