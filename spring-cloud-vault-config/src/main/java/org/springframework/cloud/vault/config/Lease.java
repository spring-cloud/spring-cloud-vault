/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.vault.config;

import org.springframework.util.Assert;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 *
 * @author Mark Paluch
 */
@EqualsAndHashCode
@ToString
class Lease {

	private final String leaseId;

	private final long leaseDuration;

	private final boolean renewable;

	private Lease(String leaseId, long leaseDuration, boolean renewable) {

		Assert.hasText(leaseId, "LeaseId must not be empty");

		this.leaseId = leaseId;
		this.leaseDuration = leaseDuration;
		this.renewable = renewable;
	}

	/**
	 * Creates a new {@link Lease}.
	 *
	 * @param leaseId must not be empty or {@literal null}.
	 * @param leaseDuration the lease duration in seconds
	 * @param renewable {@literal true} if this lease is renewable.
	 * @return the created {@link Lease}
	 */
	public static Lease of(String leaseId, long leaseDuration, boolean renewable) {
		return new Lease(leaseId, leaseDuration, renewable);
	}

	/**
	 *
	 * @return the lease Id
	 */
	public String getLeaseId() {
		return leaseId;
	}

	/**
	 *
	 * @return
	 */
	public long getLeaseDuration() {
		return leaseDuration;
	}

	/**
	 *
	 * @return {@literal true} if the lease is renewable.
	 */
	public boolean isRenewable() {
		return renewable;
	}
}
