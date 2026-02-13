package br.com.tickets.config;

import br.com.tickets.mapper.ImportacaoMapper;
import br.com.tickets.models.Importacao;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class ImportacaoJobConfiguration {

    private final PlatformTransactionManager transactionManager;

    public ImportacaoJobConfiguration(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }


    @Bean
    public Job job(Step steep1, JobRepository jobRepository){
        return new JobBuilder("gerar-tickets",jobRepository)
                .start(steep1)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public Step steep1(JobRepository jobRepository, ItemReader<Importacao> reader, ItemWriter<Importacao> writer){
        return new StepBuilder("passo-inicial",jobRepository)
                .<Importacao,Importacao>chunk(200)
                .transactionManager(transactionManager)
                .reader(reader)
                .writer(writer)
                .build();

    }

    @Bean
    public ItemReader<Importacao> reader(){
        return new FlatFileItemReaderBuilder<Importacao>()
                .name("leitura-csv")
                .resource(new FileSystemResource("files/dados.csv"))
                .comments("--")
                .delimited()
                .delimiter(";")
                .names("cpf","cliente","nascimento","evento","data","tipoIngresso","valor")
                .fieldSetMapper(new ImportacaoMapper())
                .build();
    }

    @Bean
    public ItemWriter<Importacao> writer(DataSource dataSource){
        return new JdbcBatchItemWriterBuilder<Importacao>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO importacao (id, cliente, cpf,data,evento,hora_importacao,nascimento,tipo_ingresso,valor)
                        VALUES
                        (nextval('importacao_id_seq'),:cliente,:cpf,:data,:evento,now(),:nascimento,:tipoIngresso,:valor)
                        """).itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
    }
}
