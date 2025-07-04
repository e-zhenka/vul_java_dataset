@Override
  protected void process(final ClusterDistributionManager dm) {
    Throwable thr = null;
    ReplyException rex = null;
    if (this.functionObject == null) {
      rex = new ReplyException(
          new FunctionException(LocalizedStrings.ExecuteFunction_FUNCTION_NAMED_0_IS_NOT_REGISTERED
              .toLocalizedString(this.functionName)));

      replyWithException(dm, rex);
      return;
    }

    FunctionStats stats =
        FunctionStats.getFunctionStats(this.functionObject.getId(), dm.getSystem());
    TXStateProxy tx = null;
    InternalCache cache = dm.getCache();

    try {
      tx = prepForTransaction(dm);
      ResultSender resultSender = new MemberFunctionResultSender(dm, this, this.functionObject);
      Set<Region> regions = new HashSet<Region>();
      if (this.regionPathSet != null) {
        for (String regionPath : this.regionPathSet) {
          if (checkCacheClosing(dm) || checkDSClosing(dm)) {
            if (dm.getCache() == null) {
              thr = new CacheClosedException(
                  LocalizedStrings.PartitionMessage_REMOTE_CACHE_IS_CLOSED_0
                      .toLocalizedString(dm.getId()));
            } else {
              dm.getCache().getCacheClosedException(
                  LocalizedStrings.PartitionMessage_REMOTE_CACHE_IS_CLOSED_0
                      .toLocalizedString(dm.getId()));
            }
            return;
          }
          regions.add(cache.getRegion(regionPath));
        }
      }
      FunctionContextImpl context = new MultiRegionFunctionContextImpl(cache,
          this.functionObject.getId(), this.args, resultSender, regions, isReExecute);

      long start = stats.startTime();
      stats.startFunctionExecution(this.functionObject.hasResult());
      if (logger.isDebugEnabled()) {
        logger.debug("Executing Function: {} on remote member with context: {}",
            this.functionObject.getId(), context.toString());
      }
      this.functionObject.execute(context);
      if (!this.replyLastMsg && this.functionObject.hasResult()) {
        throw new FunctionException(
            LocalizedStrings.ExecuteFunction_THE_FUNCTION_0_DID_NOT_SENT_LAST_RESULT
                .toString(functionObject.getId()));
      }
      stats.endFunctionExecution(start, this.functionObject.hasResult());
    } catch (FunctionException functionException) {
      if (logger.isDebugEnabled()) {
        logger.debug("FunctionException occurred on remote member while executing Function: {}",
            this.functionObject.getId(), functionException);
      }
      stats.endFunctionExecutionWithException(this.functionObject.hasResult());
      rex = new ReplyException(functionException);
      replyWithException(dm, rex);
      // thr = functionException.getCause();
    } catch (CancelException exception) {
      // bug 37026: this is too noisy...
      // throw new CacheClosedException("remote system shutting down");
      // thr = se; cache is closed, no point trying to send a reply
      thr = new FunctionInvocationTargetException(exception);
      stats.endFunctionExecutionWithException(this.functionObject.hasResult());
      rex = new ReplyException(thr);
      replyWithException(dm, rex);
    } catch (Exception exception) {
      logger.error("Exception occurred on remote member while executing Function: {}",
          this.functionObject.getId(), exception);

      stats.endFunctionExecutionWithException(this.functionObject.hasResult());
      rex = new ReplyException(exception);
      replyWithException(dm, rex);
      // thr = e.getCause();
    } catch (VirtualMachineError err) {
      SystemFailure.initiateFailure(err);
      // If this ever returns, rethrow the error. We're poisoned
      // now, so don't let this thread continue.
      throw err;
    } catch (Throwable t) {
      // Whenever you catch Error or Throwable, you must also
      // catch VirtualMachineError (see above). However, there is
      // _still_ a possibility that you are dealing with a cascading
      // error condition, so you also need to check to see if the JVM
      // is still usable:
      SystemFailure.checkFailure();
      thr = t;
    } finally {
      cleanupTransaction(tx);
      if (thr != null) {
        rex = new ReplyException(thr);
        replyWithException(dm, rex);
      }
    }
  }