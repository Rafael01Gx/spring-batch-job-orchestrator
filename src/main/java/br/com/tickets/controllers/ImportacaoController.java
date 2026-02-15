package br.com.tickets.controllers;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImportacaoController {

    private final JobOperator jobOperator;
    private final Job importacaoJob;


    public ImportacaoController(JobOperator jobOperator,Job importacaoJob) {
        this.jobOperator = jobOperator;
        this.importacaoJob = importacaoJob;
    }

    @GetMapping("/importar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void importarArquivo() throws JobInstanceAlreadyCompleteException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException {
        JobParameters parameters = new JobParameters();
        JobExecution executionId = jobOperator.start(importacaoJob, parameters);
    }

}
