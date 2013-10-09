package sk.baka.webvm.analyzer.utils;

import java.util.Objects;

/**
 * Meno servisu. Kazdy service ma ownera, ktory je zodpovedny za ukoncenie tohto servisu. Tato trieda explicitne pomenuva tohto ownera.
 * Kedze owner moze mat svojho ownera atd, meno ownera je oddelovane lomitkami.
 * @author Martin Vysny
 */
public final class ServiceName {
    /**
     * Service name. Hierarchical, separated with slashes. Ma tvar owner/owner/owner/.../service_name
     */
    public final String name;
    /**
     * Thready su pomenovane: {@link #name}-{@code ordinal}, aby bolo jasne, ktoremu servisu thread patri, a aka je hierarchia ownershipov.
     * Tato metoda vynucuje tuto konvenciu.
     * @param ordinal poradove cislo threadu. Staci pouzit jednoduchy AtomicInteger auto-increment.
     * @return meno threadu v tvare {@link #name}-{@code ordinal}
     */
    public String getThreadName(int ordinal) {
        return name + "-" + ordinal;
    }

    /**
     * Creates a "root" service - a service with no owner.
     * @param name the name, not null. Staci {@link Class#getSimpleName()} ak je dostatocne jednoznacne (z mena sa da jednoznacne identifikovat trieda).
     * @return service name.
     */
    public static ServiceName root(String name) {
        return new ServiceName(name);
    }
    
    private ServiceName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ServiceName other = (ServiceName) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }
    
    /**
     * Zretazi name + lomitko + service. Vytvori vlastne pomenovanie subservisu.
     * @param serviceName service, not null.
     * @return owner, never null.
     */
    public ServiceName add(String serviceName) {
        return new ServiceName(name + "/" + Objects.requireNonNull(serviceName));
    }
}
