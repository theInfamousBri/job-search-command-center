package com.brianna.jobsearch.service;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.repository.ApplicationContactRepository;
import com.brianna.jobsearch.repository.CompanyManagementRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationContactService {

    private final ApplicationContactRepository repository;
    private final JobApplicationRepository applicationRepository;
    private final CompanyManagementRepository companyRepository;

    public ApplicationContactService(
            ApplicationContactRepository repository,
            JobApplicationRepository applicationRepository,
            CompanyManagementRepository companyRepository) {
        this.repository = repository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
    }

    public List<CompanyContact> forApplication(long applicationId) {
        requireApplication(applicationId);
        return repository.findByApplicationId(applicationId);
    }

    public List<CompanyContact> linkableForApplication(long applicationId) {
        JobApplication application = requireApplication(applicationId);
        String companyKey = CompanyManagementService.normalizeCompanyKey(application.getCompany());
        if (companyKey.isBlank()) {
            return List.of();
        }
        return repository.findLinkableForApplication(applicationId, companyKey);
    }

    @Transactional
    public boolean link(long applicationId, long contactId) {
        JobApplication application = requireApplication(applicationId);
        CompanyContact contact = requireContact(contactId);
        String applicationCompanyKey = CompanyManagementService.normalizeCompanyKey(application.getCompany());
        if (!applicationCompanyKey.equals(contact.companyKey())) {
            throw new IllegalArgumentException(
                    contact.name() + " belongs to a different company and cannot be linked to this application.");
        }
        return repository.link(applicationId, contactId);
    }

    @Transactional
    public boolean unlink(long applicationId, long contactId) {
        requireApplication(applicationId);
        requireContact(contactId);
        return repository.unlink(applicationId, contactId) > 0;
    }

    private JobApplication requireApplication(long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    private CompanyContact requireContact(long contactId) {
        CompanyContact contact = companyRepository.findContact(contactId);
        if (contact == null) {
            throw new ResourceNotFoundException("Person not found: " + contactId);
        }
        return contact;
    }
}
