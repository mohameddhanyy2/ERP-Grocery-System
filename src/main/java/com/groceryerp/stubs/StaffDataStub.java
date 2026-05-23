package com.groceryerp.stubs;

import com.groceryerp.interfaces.IStaffData;
import java.util.Arrays;
import java.util.List;

/**
 * Simple stub for IStaffData used in demos/tests.
 */
public class StaffDataStub implements IStaffData {

    @Override
    public List<String> getStaffIdsByStore(String storeId) {
        return Arrays.asList("s1", "s2", "s3");
    }

    @Override
    public double getTotalPayrollCost(String period) {
        // Return a fixed payroll cost for demo
        return 4000.0;
    }

    @Override
    public int getStaffCount(String storeId) {
        return 3;
    }
}
