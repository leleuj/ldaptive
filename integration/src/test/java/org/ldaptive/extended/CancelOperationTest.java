/* See LICENSE for licensing and NOTICE for copyright. */
package org.ldaptive.extended;

import org.ldaptive.AbstractTest;
import org.ldaptive.Connection;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.TestControl;
import org.ldaptive.TestUtils;
import org.ldaptive.async.AsyncSearchOperation;
import org.ldaptive.control.SyncRequestControl;
import org.ldaptive.handler.HandlerResult;
import org.ldaptive.handler.SearchEntryHandler;
import org.testng.AssertJUnit;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Unit test for {@link CancelOperation}.
 *
 * @author  Middleware Services
 */
public class CancelOperationTest extends AbstractTest
{


  /**
   * @param  dn  to search on.
   *
   * @throws  Exception  On test failure.
   */
  @Parameters("cancelDn")
  @Test(groups = {"extended"})
  public void cancel(final String dn)
    throws Exception
  {
    // provider doesn't support cancel
    if (TestControl.isOpenDJProvider()) {
      throw new UnsupportedOperationException("OpenDJ does not support cancel");
    }

    try (Connection conn = TestUtils.createConnection()) {
      conn.open();

      final AsyncSearchOperation search = new AsyncSearchOperation(conn);
      // needed to perform operations inside a handler
      search.setUseMultiThreadedListener(true);
      search.setExceptionHandler(
        (conn1, request, exception) -> {
          throw new UnsupportedOperationException(exception);
        });

      final SearchRequest request = SearchRequest.newObjectScopeSearchRequest(dn);
      request.setSearchEntryHandlers(
        new SearchEntryHandler() {
          @Override
          public HandlerResult<SearchEntry> handle(
            final Connection conn,
            final SearchRequest request,
            final SearchEntry entry)
            throws LdapException
          {
            try {
              final CancelOperation cancel = new CancelOperation(conn);
              final Response<Void> response = cancel.execute(new CancelRequest(entry.getMessageId()));
              AssertJUnit.assertEquals(ResultCode.SUCCESS, response.getResultCode());
              return new HandlerResult<>(null);
            } catch (LdapException e) {
              return new HandlerResult<>(null, true);
            }
          }

          @Override
          public void initializeRequest(final SearchRequest request) {}
        });
      request.setControls(new SyncRequestControl(SyncRequestControl.Mode.REFRESH_AND_PERSIST, true));

      final Response<SearchResult> response = search.execute(request);
      AssertJUnit.assertEquals(ResultCode.CANCELED, response.getResultCode());
    } catch (IllegalStateException e) {
      throw (Exception) e.getCause();
    }
  }
}
