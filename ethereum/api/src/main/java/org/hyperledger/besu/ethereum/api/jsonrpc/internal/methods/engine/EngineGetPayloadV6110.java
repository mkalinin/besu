/*
 * Copyright Hyperledger Besu Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.engine;

import io.vertx.core.Vertx;
import org.hyperledger.besu.consensus.merge.blockcreation.MergeMiningCoordinator;
import org.hyperledger.besu.consensus.merge.blockcreation.PayloadIdentifier;
import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.RpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.hyperledger.besu.ethereum.core.BlockWithReceipts;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.ethereum.mainnet.ScheduledProtocolSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineGetPayloadV6110 extends AbstractEngineGetPayload {

  private static final Logger LOG = LoggerFactory.getLogger(EngineGetPayloadV6110.class);
  private final ScheduledProtocolSpec.Hardfork shanghai;
  private final ScheduledProtocolSpec.Hardfork cancun;
  private final ScheduledProtocolSpec.Hardfork experimentalEips;

  public EngineGetPayloadV6110(
      final Vertx vertx,
      final ProtocolContext protocolContext,
      final MergeMiningCoordinator mergeMiningCoordinator,
      final BlockResultFactory blockResultFactory,
      final EngineCallListener engineCallListener,
      final ProtocolSchedule schedule) {
    super(
        vertx,
        protocolContext,
        mergeMiningCoordinator,
        blockResultFactory,
        engineCallListener,
        schedule);
    this.shanghai = schedule.hardforkFor(s -> s.fork().name().equalsIgnoreCase("Shanghai"));
    this.cancun = schedule.hardforkFor(s -> s.fork().name().equalsIgnoreCase("Cancun"));
    this.experimentalEips =
        schedule.hardforkFor(s -> s.fork().name().equalsIgnoreCase("ExperimentalEips"));
  }

  @Override
  public String getName() {
    return RpcMethod.ENGINE_GET_PAYLOAD_V6110.getMethodName();
  }

  @Override
  protected JsonRpcResponse createResponse(
      final JsonRpcRequestContext request,
      final PayloadIdentifier payloadId,
      final BlockWithReceipts blockWithReceipts) {

    try {
      long builtAt = blockWithReceipts.getHeader().getTimestamp();

      if (builtAt < this.shanghai.milestone()) {
        return new JsonRpcSuccessResponse(
            request.getRequest().getId(),
            blockResultFactory.payloadTransactionCompleteV1(blockWithReceipts.getBlock()));
      } else if (builtAt >= this.shanghai.milestone() && builtAt < this.cancun.milestone()) {
        return new JsonRpcSuccessResponse(
            request.getRequest().getId(),
            blockResultFactory.payloadTransactionCompleteV2(blockWithReceipts));
      } else if (builtAt >= this.cancun.milestone() && builtAt < this.experimentalEips.milestone()) {
        return new JsonRpcSuccessResponse(
            request.getRequest().getId(),
            blockResultFactory.payloadTransactionCompleteV3(blockWithReceipts));
      } else {
        return new JsonRpcSuccessResponse(
            request.getRequest().getId(),
            blockResultFactory.payloadTransactionCompleteV6110(blockWithReceipts));
      }

    } catch (ClassCastException e) {
      LOG.error("configuration error, can't call V6110 endpoint with non-default protocol schedule");
      return new JsonRpcErrorResponse(request.getRequest().getId(), JsonRpcError.INTERNAL_ERROR);
    }
  }
}