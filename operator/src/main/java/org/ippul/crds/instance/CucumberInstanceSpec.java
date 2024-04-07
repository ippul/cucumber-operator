package org.ippul.crds.instance;

public class CucumberInstanceSpec {

    private String persistenceVolumeClaimName;

    public String getPersistenceVolumeClaimName() {
        return persistenceVolumeClaimName;
    }

    public void setPersistenceVolumeClaimName(String persistenceVolumeClaimName) {
        this.persistenceVolumeClaimName = persistenceVolumeClaimName;
    }

	@Override
	public String toString() {
		return "CucumberInstanceSpec [persistenceVolumeClaimName="
				+ persistenceVolumeClaimName + "]";
	}
    
}
