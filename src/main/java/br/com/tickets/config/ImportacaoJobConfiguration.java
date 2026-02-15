package br.com.tickets.config;

import br.com.tickets.mapper.ImportacaoMapper;
import br.com.tickets.models.Importacao;
import br.com.tickets.services.ImportacaoProcessor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.stream.Stream;

/**
 * Configuração do Job de importação de tickets usando Spring Batch 5.x
 * compatível com Spring Framework 7.x
 */
@Configuration
public class ImportacaoJobConfiguration {

    private static final String JOB_NAME = "gerar-tickets";
    private static final String STEP_PROCESSING = "processar-importacao";
    private static final String STEP_MOVE_FILES = "mover-arquivos";
    private static final int CHUNK_SIZE = 200;

    @Value("${batch.import.source-path:files}")
    private String sourcePath;

    @Value("${batch.import.target-path:imported-files}")
    private String targetPath;

    @Value("${batch.import.file-pattern:dados.csv}")
    private String fileName;

    /**
     * Definição do Job principal
     */
    @Bean
    public Job importacaoJob(JobRepository jobRepository,
                             Step processingStep,
                             Step moveFilesStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(processingStep)
                .next(moveFilesStep)
                .build();
    }

    /**
     * Step de processamento dos dados do CSV
     */
    @Bean
    public Step processingStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               ItemReader<Importacao> importacaoReader,
                               ItemProcessor<Importacao, Importacao> importacaoProcessor,
                               ItemWriter<Importacao> importacaoWriter) {
        return new StepBuilder(STEP_PROCESSING, jobRepository)
                .<Importacao, Importacao>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(importacaoReader)
                .processor(importacaoProcessor)
                .writer(importacaoWriter)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    /**
     * Step para mover arquivos processados
     */
    @Bean
    public Step moveFilesStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_MOVE_FILES, jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    moveProcessedFiles();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * Reader para ler o arquivo CSV
     */
    @Bean
    public FlatFileItemReader<Importacao> importacaoReader() {
        return new FlatFileItemReaderBuilder<Importacao>()
                .name("importacao-csv-reader")
                .resource(getFileResource())
                .linesToSkip(0)
                .comments("--", "#")
                .delimited()
                .delimiter(";")
                .names("cpf", "cliente", "nascimento", "evento", "data", "tipoIngresso", "valor")
                .fieldSetMapper(new ImportacaoMapper())
                .strict(true)
                .saveState(true)
                .build();
    }

    /**
     * Processor para processar os dados
     */
    @Bean
    public ItemProcessor<Importacao, Importacao> importacaoProcessor() {
        return new ImportacaoProcessor();
    }

    /**
     * Writer para inserir dados no banco
     */
    @Bean
    public JdbcBatchItemWriter<Importacao> importacaoWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Importacao>()
                .dataSource(dataSource)
                .sql("""
                    INSERT INTO importacao (
                        id, cliente, cpf, data, evento, 
                        hora_importacao, nascimento, taxa_admin, 
                        tipo_ingresso, valor
                    ) VALUES (
                        nextval('importacao_id_seq'), 
                        :cliente, :cpf, :data, :evento, 
                        CURRENT_TIMESTAMP, :nascimento, :taxaAdmin, 
                        :tipoIngresso, :valor
                    )
                    """)
                .beanMapped()
                .assertUpdates(true)
                .build();
    }

    /**
     * Obtém o resource do arquivo de importação
     */
    private Resource getFileResource() {
        Path filePath = Paths.get(sourcePath, fileName);
        return new org.springframework.core.io.FileSystemResource(filePath);
    }

    /**
     * Move os arquivos processados para pasta de destino
     */
    private void moveProcessedFiles() throws IOException {
        Path sourceDir = Paths.get(sourcePath);
        Path targetDir = Paths.get(targetPath);

        // Cria o diretório de destino se não existir
        Files.createDirectories(targetDir);

        // Processa arquivos CSV
        try (Stream<Path> files = Files.list(sourceDir)) {
            files.filter(path -> path.toString().endsWith(".csv"))
                    .forEach(sourceFile -> {
                        try {
                            String timestamp = LocalDate.now().toString();
                            String newFileName =  timestamp + "_" + sourceFile.getFileName();
                            Path targetFile = targetDir.resolve(newFileName);

                            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

                            System.out.printf("Arquivo movido com sucesso: %s -> %s%n",
                                    sourceFile.getFileName(), targetFile.getFileName());
                        } catch (IOException e) {
                            String errorMsg = String.format("Erro ao mover arquivo: %s",
                                    sourceFile.getFileName());
                            System.err.println(errorMsg);
                            throw new RuntimeException(errorMsg, e);
                        }
                    });
        }
    }
}