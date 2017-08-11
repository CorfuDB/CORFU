package org.corfudb.runtime.view;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * hold address-space specific options.
 *
 * Created by dmalkhi on 7/13/17.
 */
@AllArgsConstructor @NoArgsConstructor
public class AddressSpaceOptions {
    @Getter
    @Setter
    boolean doCache = true;
}
