package com.boltpe.insurance.client;

import com.boltpe.insurance.config.PolicyPlannerProperties;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.vendor.request.HealthVendorRequest;
import com.boltpe.insurance.vendor.request.SuperTopUpVendorRequest;
import com.boltpe.insurance.vendor.request.PersonalAccidentVendorRequest;
import com.boltpe.insurance.vendor.request.HospitalCashVendorRequest;
import com.boltpe.insurance.vendor.response.*;
import java.net.URI;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PolicyPlannerClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyPlannerClient.class);
    private final PolicyPlannerHttpGateway gateway;
    private final PolicyPlannerProperties properties;
    private final URI baseUri;

    public PolicyPlannerClient(PolicyPlannerHttpGateway gateway, PolicyPlannerProperties properties) {
        this.gateway = gateway; this.properties = properties; this.baseUri = URI.create(properties.baseUrl());
    }

    /** Backward-compatible method used by existing callers/tests. */
    public List<HealthVendorResponse> fetchHealthQuotes(HealthVendorRequest request) {
        HealthQuoteBatch batch = fetchHealthQuoteBatch(request);
        if (batch.quotes().isEmpty()) {
            throw new PolicyPlannerException("No Policy Planner health provider returned a quote");
        }
        return batch.quotes();
    }

    /** Returns successful quotes plus the true/false status of every discovered premium API. */
    /** Gets quotes while retaining an API status for every discoverable, non-blank provider URL. */
    public HealthQuoteBatch fetchHealthQuoteBatch(HealthVendorRequest request) {
        HealthDiscoveryResponse discovery = discoverHealthPlans();
        List<HealthPlanApiDescriptor> plans = discovery.data();
        LOGGER.info("Policy Planner health discovery returned count={}, actualPlanApis={}", discovery.count(), plans.size());
        List<HealthVendorResponse> responses = new ArrayList<>();
        List<HealthQuoteBatch.ApiStatus> statuses = new ArrayList<>();
        int skipped = 0;
        for (HealthPlanApiDescriptor descriptor : plans) {
            if (descriptor == null || descriptor.api_type() == null || descriptor.api_type().isBlank()) {
                skipped++;
                LOGGER.warn("Skipping Policy Planner health plan because premium URL is blank");
                continue;
            }
            statuses.add(fetchHealthPremium(descriptor.api_type(), request, responses));
        }
        long successes = statuses.stream().filter(HealthQuoteBatch.ApiStatus::active).count();
        LOGGER.info("Policy Planner Health quote summary: discovered={}, attempted={}, success={}, failed={}, skipped={}",
                plans.size(), statuses.size(), successes, statuses.size() - successes, skipped);
        return new HealthQuoteBatch(List.copyOf(responses), List.copyOf(statuses));
    }

    private HealthQuoteBatch.ApiStatus fetchHealthPremium(String url, HealthVendorRequest request,
                                                            List<HealthVendorResponse> responses) {
        String[] ids = extractIds(url);
        try {
            HealthVendorResponse response = gateway.fetchHealthQuote(validateAndNormalizeVendorUrl(url), request);
            if (response == null) {
                LOGGER.warn("Policy Planner quote returned empty response for premiumUrl={}", url);
                return new HealthQuoteBatch.ApiStatus(url, false, ids[0], ids[1], "Empty response");
            }
            responses.add(response);
            LOGGER.info("Policy Planner quote SUCCESS for premiumUrl={}, companyId={}, planId={}, planName={}",
                    url, response.companyId(), response.planId(), response.planName());
            return new HealthQuoteBatch.ApiStatus(url, true, firstNonBlank(response.companyId(), ids[0]),
                    firstNonBlank(response.planId(), ids[1]), null);
        } catch (RuntimeException ex) {
            String reason = extractErrorMessage(ex);
            LOGGER.warn("Policy Planner quote FAILED for premiumUrl={}, reason={}", url, reason);
            LOGGER.debug("Policy Planner quote failure details for premiumUrl={}", url, ex);
            return new HealthQuoteBatch.ApiStatus(url, false, ids[0], ids[1], reason);
        }
    }


    public SuperTopUpQuoteBatch fetchSuperTopUpQuoteBatch(SuperTopUpVendorRequest request) {
        SuperTopUpDiscoveryResponse discovery = discoverSuperTopUpPlans();
        List<SuperTopUpVendorResponse> responses = new ArrayList<>();
        List<SuperTopUpQuoteBatch.ApiStatus> statuses = new ArrayList<>();
        for (SuperTopUpPlanApiDescriptor descriptor : discovery.data()) {
            if (descriptor == null || descriptor.api_type() == null || descriptor.api_type().isBlank()) {
                continue;
            }
            statuses.add(fetchSuperTopUpPremium(descriptor.api_type(), request, responses));
        }
        return new SuperTopUpQuoteBatch(List.copyOf(responses), List.copyOf(statuses));
    }

    private SuperTopUpDiscoveryResponse discoverSuperTopUpPlans() {
        try {
            SuperTopUpDiscoveryResponse discovery = gateway.discoverSuperTopUpPlans(properties.superTopUp().discoveryPath());
            if (discovery == null || !discovery.success() || discovery.data().isEmpty()) {
                throw new PolicyPlannerException("Policy Planner returned no Super Top-Up plan APIs");
            }
            return discovery;
        } catch (PolicyPlannerException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PolicyPlannerException("Unable to discover Policy Planner Super Top-Up plans", ex);
        }
    }

    private SuperTopUpQuoteBatch.ApiStatus fetchSuperTopUpPremium(String url, SuperTopUpVendorRequest request,
                                                                    List<SuperTopUpVendorResponse> responses) {
        String[] ids = extractSuperTopUpIds(url);
        try {
            SuperTopUpVendorResponse response = gateway.fetchSuperTopUpQuote(validateAndNormalizeVendorUrl(url), request);
            if (isSuccessfulSuperTopUpQuote(response)) {
                responses.add(response);
                return new SuperTopUpQuoteBatch.ApiStatus(url, true, firstNonBlank(response.companyId(), ids[0]),
                        firstNonBlank(response.planId(), ids[1]), null);
            }
            String reason = response != null && response.error() != null ? response.error() : "No premiums found";
            return new SuperTopUpQuoteBatch.ApiStatus(url, false, ids[0], ids[1], reason);
        } catch (RuntimeException ex) {
            return new SuperTopUpQuoteBatch.ApiStatus(url, false, ids[0], ids[1], extractErrorMessage(ex));
        }
    }

    private static boolean isSuccessfulSuperTopUpQuote(SuperTopUpVendorResponse response) {
        return response != null && (response.error() == null || response.error().isBlank())
                && response.premiums() != null && !response.premiums().isEmpty();
    }



    public PersonalAccidentQuoteBatch fetchPersonalAccidentQuoteBatch(PersonalAccidentVendorRequest request) {
        PersonalAccidentDiscoveryResponse discovery = discoverPersonalAccidentPlans();
        List<PersonalAccidentVendorResponse> responses = new ArrayList<>();
        List<PersonalAccidentQuoteBatch.ApiStatus> statuses = new ArrayList<>();
        for (PersonalAccidentPlanApiDescriptor descriptor : discovery.data()) {
            if (descriptor == null || descriptor.api_type() == null || descriptor.api_type().isBlank()) {
                continue;
            }
            statuses.add(fetchPersonalAccidentPremium(descriptor.api_type(), request, responses));
        }
        return new PersonalAccidentQuoteBatch(List.copyOf(responses), List.copyOf(statuses));
    }

    private PersonalAccidentDiscoveryResponse discoverPersonalAccidentPlans() {
        try {
            PersonalAccidentDiscoveryResponse discovery = gateway.discoverPersonalAccidentPlans(
                    properties.personalAccident().discoveryPath());
            if (discovery == null || !discovery.success() || discovery.data().isEmpty()) {
                throw new PolicyPlannerException("Policy Planner returned no Personal Accident plan APIs");
            }
            return discovery;
        } catch (PolicyPlannerException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PolicyPlannerException("Unable to discover Policy Planner Personal Accident plans", ex);
        }
    }

    private PersonalAccidentQuoteBatch.ApiStatus fetchPersonalAccidentPremium(String url,
            PersonalAccidentVendorRequest request, List<PersonalAccidentVendorResponse> responses) {
        String[] ids = extractPersonalAccidentIds(url);
        try {
            PersonalAccidentVendorResponse response = gateway.fetchPersonalAccidentQuote(
                    validateAndNormalizeVendorUrl(url), request);
            if (isSuccessfulPersonalAccidentQuote(response)) {
                responses.add(response);
                return new PersonalAccidentQuoteBatch.ApiStatus(url, true, ids[0], ids[1], null);
            }
            String reason = response != null && response.error() != null ? response.error() : "No premiums found";
            return new PersonalAccidentQuoteBatch.ApiStatus(url, false, ids[0], ids[1], reason);
        } catch (RuntimeException ex) {
            return new PersonalAccidentQuoteBatch.ApiStatus(url, false, ids[0], ids[1], extractErrorMessage(ex));
        }
    }

    private static boolean isSuccessfulPersonalAccidentQuote(PersonalAccidentVendorResponse response) {
        return response != null && (response.error() == null || response.error().isBlank())
                && response.premiums() != null && !response.premiums().isEmpty();
    }


    public HospitalCashQuoteBatch fetchHospitalCashQuoteBatch(HospitalCashVendorRequest request) {
        HospitalCashDiscoveryResponse discovery = discoverHospitalCashPlans();
        List<HospitalCashVendorResponse> responses = new ArrayList<>();
        List<HospitalCashQuoteBatch.ApiStatus> statuses = new ArrayList<>();
        for (HospitalCashPlanApiDescriptor descriptor : discovery.data()) {
            if (descriptor == null || descriptor.api_type() == null || descriptor.api_type().isBlank()) {
                continue;
            }
            statuses.add(fetchHospitalCashPremium(descriptor.api_type(), request, responses));
        }
        return new HospitalCashQuoteBatch(responses, statuses);
    }

    private HospitalCashDiscoveryResponse discoverHospitalCashPlans() {
        try {
            HospitalCashDiscoveryResponse discovery = gateway.discoverHospitalCashPlans(
                    properties.hospitalCash().discoveryPath());
            if (discovery == null || !discovery.success() || discovery.data().isEmpty()) {
                throw new PolicyPlannerException("Policy Planner returned no Hospital Cash plan APIs");
            }
            return discovery;
        } catch (PolicyPlannerException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PolicyPlannerException("Unable to discover Policy Planner Hospital Cash plans", ex);
        }
    }

    private HospitalCashQuoteBatch.ApiStatus fetchHospitalCashPremium(String url, HospitalCashVendorRequest request,
                                                                        List<HospitalCashVendorResponse> responses) {
        String[] ids = extractHospitalCashIds(url);
        try {
            HospitalCashVendorResponse response = gateway.fetchHospitalCashQuote(
                    validateAndNormalizeVendorUrl(url), request);
            if (isSuccessfulHospitalCashQuote(response)) {
                responses.add(response);
                return new HospitalCashQuoteBatch.ApiStatus(url, true, ids[0], ids[1], null);
            }
            String reason = response != null && response.error() != null ? response.error() : "No premium found";
            return new HospitalCashQuoteBatch.ApiStatus(url, false, ids[0], ids[1], reason);
        } catch (RuntimeException ex) {
            return new HospitalCashQuoteBatch.ApiStatus(url, false, ids[0], ids[1], extractErrorMessage(ex));
        }
    }

    private static boolean isSuccessfulHospitalCashQuote(HospitalCashVendorResponse response) {
        return response != null && (response.error() == null || response.error().isBlank())
                && response.premium() != null;
    }


    private static String[] extractHospitalCashIds(String url) {
        try { String[] p=URI.create(url).getPath().split("/"); for(int i=0;i<p.length;i++) if(("hc".equalsIgnoreCase(p[i])||"hospitalcash".equalsIgnoreCase(p[i])||"hospital-cash".equalsIgnoreCase(p[i]))&&i>=2) return new String[]{p[i-2],p[i-1]}; }
        catch(RuntimeException ignored) { }
        return new String[]{null,null};
    }

    private static String[] extractPersonalAccidentIds(String url) {
        try { String[] p=URI.create(url).getPath().split("/"); for(int i=0;i<p.length;i++) if("pa".equalsIgnoreCase(p[i])&&i>=2) return new String[]{p[i-2],p[i-1]}; }
        catch(RuntimeException ignored) { }
        return new String[]{null,null};
    }

    private static String[] extractSuperTopUpIds(String url) {
        try { String[] p=URI.create(url).getPath().split("/"); for(int i=0;i<p.length;i++) if("supertopup".equalsIgnoreCase(p[i])&&i>=2) return new String[]{p[i-2],p[i-1]}; }
        catch(RuntimeException ignored) { }
        return new String[]{null,null};
    }

    private HealthDiscoveryResponse discoverHealthPlans() {
        try {
            HealthDiscoveryResponse d = gateway.discoverHealthPlans(properties.health().discoveryPath());
            if (d == null || !d.success() || d.data() == null || d.data().isEmpty())
                throw new PolicyPlannerException("Policy Planner returned no health plan APIs");
            return d;
        } catch (PolicyPlannerException ex) { throw ex; }
        catch (RuntimeException ex) { throw new PolicyPlannerException("Unable to discover Policy Planner health plans", ex); }
    }

    String validateAndNormalizeVendorUrl(String candidate) {
        URI uri = URI.create(candidate);
        if (!uri.isAbsolute()) {
            if (candidate.startsWith("//")) throw new PolicyPlannerException("Rejected scheme-relative Policy Planner premium URL");
            return candidate.startsWith("/") ? candidate : "/" + candidate;
        }
        if (!Objects.equals(baseUri.getScheme(), uri.getScheme()) || !Objects.equals(baseUri.getHost(), uri.getHost())
                || effectivePort(baseUri) != effectivePort(uri))
            throw new PolicyPlannerException("Rejected Policy Planner premium URL outside configured vendor host");
        String path = uri.getRawPath();
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }

    private static String[] extractIds(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null) path = url;
            String[] p = path.split("/");
            for (int i = 0; i < p.length; i++) {
                if ("premium".equalsIgnoreCase(p[i]) && i >= 2) return new String[]{p[i-2], p[i-1]};
            }
        } catch (RuntimeException ignored) { }
        return new String[]{null, null};
    }
    private static String firstNonBlank(String a, String b) { return a != null && !a.isBlank() ? a : b; }
    private static String extractErrorMessage(RuntimeException e) {
        if (e.getMessage()!=null && !e.getMessage().isBlank()) return e.getMessage();
        Throwable c=e.getCause(); if(c!=null && c.getMessage()!=null && !c.getMessage().isBlank()) return c.getMessage();
        return e.getClass().getSimpleName();
    }
    private static int effectivePort(URI uri) { return uri.getPort()>=0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme())?443:80); }
}
