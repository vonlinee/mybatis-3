package org.apache.ibatis.builder;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;

class ConfigurationElementHolder {

  protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<>();
  protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<>();
  protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<>();
  protected final Collection<MethodResolver> incompleteMethods = new LinkedList<>();

  private final ReentrantLock incompleteResultMapsLock = new ReentrantLock();
  private final ReentrantLock incompleteCacheRefsLock = new ReentrantLock();
  private final ReentrantLock incompleteStatementsLock = new ReentrantLock();
  private final ReentrantLock incompleteMethodsLock = new ReentrantLock();

  public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
    incompleteStatementsLock.lock();
    try {
      incompleteStatements.add(incompleteStatement);
    } finally {
      incompleteStatementsLock.unlock();
    }
  }

  public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
    incompleteCacheRefsLock.lock();
    try {
      incompleteCacheRefs.add(incompleteCacheRef);
    } finally {
      incompleteCacheRefsLock.unlock();
    }
  }

  public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
    incompleteResultMapsLock.lock();
    try {
      incompleteResultMaps.add(resultMapResolver);
    } finally {
      incompleteResultMapsLock.unlock();
    }
  }

  public void addIncompleteMethod(MethodResolver builder) {
    incompleteMethodsLock.lock();
    try {
      incompleteMethods.add(builder);
    } finally {
      incompleteMethodsLock.unlock();
    }
  }

  public void parsePendingMethods(boolean reportUnresolved) {
    if (incompleteMethods.isEmpty()) {
      return;
    }
    incompleteMethodsLock.lock();
    try {
      incompleteMethods.removeIf(x -> {
        x.resolve();
        return true;
      });
    } catch (IncompleteElementException e) {
      if (reportUnresolved) {
        throw e;
      }
    } finally {
      incompleteMethodsLock.unlock();
    }
  }

  public void parsePendingStatements(boolean reportUnresolved) {
    if (incompleteStatements.isEmpty()) {
      return;
    }
    incompleteStatementsLock.lock();
    try {
      incompleteStatements.removeIf(x -> {
        x.parseStatementNode();
        return true;
      });
    } catch (IncompleteElementException e) {
      if (reportUnresolved) {
        throw e;
      }
    } finally {
      incompleteStatementsLock.unlock();
    }
  }

  public void parsePendingCacheRefs(boolean reportUnresolved) {
    if (incompleteCacheRefs.isEmpty()) {
      return;
    }
    incompleteCacheRefsLock.lock();
    try {
      incompleteCacheRefs.removeIf(x -> x.resolveCacheRef() != null);
    } catch (IncompleteElementException e) {
      if (reportUnresolved) {
        throw e;
      }
    } finally {
      incompleteCacheRefsLock.unlock();
    }
  }

  public void parsePendingResultMaps(boolean reportUnresolved) {
    if (incompleteResultMaps.isEmpty()) {
      return;
    }
    incompleteResultMapsLock.lock();
    try {
      boolean resolved;
      IncompleteElementException ex = null;
      do {
        resolved = false;
        Iterator<ResultMapResolver> iterator = incompleteResultMaps.iterator();
        while (iterator.hasNext()) {
          try {
            iterator.next().resolve();
            iterator.remove();
            resolved = true;
          } catch (IncompleteElementException e) {
            ex = e;
          }
        }
      } while (resolved);
      if (reportUnresolved && !incompleteResultMaps.isEmpty() && ex != null) {
        // At least one result map is unresolvable.
        throw ex;
      }
    } finally {
      incompleteResultMapsLock.unlock();
    }
  }
}
