package busrouting.configLoading;

import java.util.List;

public class FileExceptions {
    private List<Integer> removeStoppingPointsIdentifiers;
    private List<TransferException> transferExceptions;
    private List<LocationNameOverlap> locationNameOverlaps;

    public FileExceptions() {
    }

    public List<LocationNameOverlap> getLocationNameOverlaps() {
        return locationNameOverlaps;
    }

    public void setLocationNameOverlaps(List<LocationNameOverlap> locationNameOverlaps) {
        this.locationNameOverlaps = locationNameOverlaps;
    }

    public List<Integer> getRemoveStoppingPointsIdentifiers() {
        return removeStoppingPointsIdentifiers;
    }

    public void setRemoveStoppingPointsIdentifiers(List<Integer> removeStoppingPointsIdentifiers) {
        this.removeStoppingPointsIdentifiers = removeStoppingPointsIdentifiers;
    }

    public List<TransferException> getTransferExceptions() {
        return transferExceptions;
    }

    public void setTransferExceptions(List<TransferException> transferExceptions) {
        this.transferExceptions = transferExceptions;
    }
}
